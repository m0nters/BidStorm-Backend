package com.taitrinh.online_auction.service;

import java.time.ZonedDateTime;
import java.util.Optional;

import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import com.stripe.exception.StripeException;
import com.stripe.model.PaymentIntent;
import com.taitrinh.online_auction.dto.order.OrderStatusResponse;
import com.taitrinh.online_auction.dto.order.PaymentIntentResponse;
import com.taitrinh.online_auction.dto.websocket.OrderStatusEvent;
import com.taitrinh.online_auction.entity.OrderCompletion;
import com.taitrinh.online_auction.entity.OrderCompletion.OrderStatus;
import com.taitrinh.online_auction.entity.Product;
import com.taitrinh.online_auction.exception.BadRequestException;
import com.taitrinh.online_auction.exception.ResourceNotFoundException;
import com.taitrinh.online_auction.exception.UnauthorizedSellerException;
import com.taitrinh.online_auction.mapper.OrderCompletionMapper;
import com.taitrinh.online_auction.repository.OrderCompletionRepository;
import com.taitrinh.online_auction.repository.ProductRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class OrderCompletionService {

    private final OrderCompletionRepository orderCompletionRepository;
    private final ProductRepository productRepository;
    private final ProfileService profileService;
    private final StripePaymentService stripePaymentService;
    private final SimpMessagingTemplate messagingTemplate;
    private final OrderCompletionMapper orderCompletionMapper;

    /**
     * Create order after auction ends
     * Can be called by either seller or winner
     */
    @Transactional
    public OrderStatusResponse createOrder(Long productId, Long userId) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product", productId));

        // Verify product has ended and has winner
        if (!product.getIsEnded()) {
            throw new BadRequestException("Sản phẩm chưa kết thúc đấu giá");
        }

        if (product.getWinner() == null) {
            throw new BadRequestException("Sản phẩm không có người thắng cuộc");
        }

        // Verify caller is either seller or winner
        boolean isSeller = product.getSeller() != null && product.getSeller().getId().equals(userId);
        boolean isWinner = product.getWinner().getId().equals(userId);

        if (!isSeller && !isWinner) {
            throw new UnauthorizedSellerException(
                    "Chỉ người bán hoặc người thắng cuộc mới có thể truy cập vào trang này");
        }

        // Check if order already exists - if so, return it instead of creating
        // duplicate
        Optional<OrderCompletion> existingOrder = orderCompletionRepository.findByProduct_Id(productId);
        if (existingOrder.isPresent()) {
            log.info("Order already exists for product {}, returning existing order {}",
                    productId, existingOrder.get().getId());
            return orderCompletionMapper.toOrderStatusResponse(existingOrder.get());
        }

        // Use product price directly in VND (Stripe's smallest unit for VND is đồng)
        Long amountVnd = product.getCurrentPrice().longValue();

        OrderCompletion order = OrderCompletion.builder()
                .product(product)
                .winner(product.getWinner())
                .status(OrderStatus.PENDING_PAYMENT)
                .amountCents(amountVnd)
                .currency("VND")
                .build();

        order = orderCompletionRepository.save(order);

        log.info("Created order {} for product {} by user {}", order.getId(), productId, userId);

        return orderCompletionMapper.toOrderStatusResponse(order);
    }

    /**
     * Initiate payment - create Stripe PaymentIntent
     */
    @Transactional
    public PaymentIntentResponse initiatePayment(Long orderId, String shippingAddress, String shippingPhone,
            Long buyerId) {
        OrderCompletion order = orderCompletionRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order", orderId));

        // Verify buyer
        if (!order.getWinner().getId().equals(buyerId)) {
            throw new UnauthorizedSellerException("Bạn không phải là người mua hàng");
        }

        // Verify status
        if (order.getStatus() != OrderStatus.PENDING_PAYMENT) {
            throw new BadRequestException("Đơn hàng không ở trạng thái chờ thanh toán");
        }

        // Update shipping info
        order.setShippingAddress(shippingAddress);
        order.setShippingPhone(shippingPhone);
        orderCompletionRepository.save(order);

        try {
            // Create Stripe PaymentIntent
            PaymentIntent paymentIntent = stripePaymentService.createPaymentIntent(order);

            // Save PaymentIntent ID
            order.setStripePaymentIntentId(paymentIntent.getId());
            orderCompletionRepository.save(order);

            // Schedule broadcast AFTER transaction commits
            TransactionSynchronizationManager.registerSynchronization(
                    new TransactionSynchronization() {
                        @Override
                        public void afterCommit() {
                            broadcastStatusUpdate(order, "Người mua đã cung cấp địa chỉ giao hàng");
                        }
                    });

            return PaymentIntentResponse.builder()
                    .clientSecret(paymentIntent.getClientSecret())
                    .amountCents(order.getAmountCents())
                    .currency(order.getCurrency())
                    .build();

        } catch (StripeException e) {
            log.error("Failed to create PaymentIntent for order {}: {}", orderId, e.getMessage());
            throw new RuntimeException("Không thể tạo thanh toán: " + e.getMessage());
        }
    }

    /**
     * Handle successful payment from webhook
     */
    @Transactional
    public void handlePaymentSuccess(String paymentIntentId) {
        OrderCompletion order = orderCompletionRepository.findByStripePaymentIntentId(paymentIntentId)
                .orElseThrow(() -> new ResourceNotFoundException("Order with PaymentIntent", paymentIntentId));

        order.setStatus(OrderStatus.PAID);
        order.setPaidAt(ZonedDateTime.now());
        orderCompletionRepository.save(order);

        log.info("Order {} payment succeeded", order.getId());

        // Broadcast status update
        broadcastStatusUpdate(order, "Thanh toán thành công, chờ người bán gửi hàng");
    }

    /**
     * Seller marks as shipped
     */
    @Transactional
    public OrderStatusResponse markAsShipped(Long orderId, String trackingNumber, Long sellerId) {
        OrderCompletion order = orderCompletionRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order", orderId));

        // Verify seller
        if (!order.getProduct().getSeller().getId().equals(sellerId)) {
            throw new UnauthorizedSellerException("Bạn không phải là người bán hàng");
        }

        // Verify status
        if (order.getStatus() != OrderStatus.PAID) {
            throw new BadRequestException("Đơn hàng chưa được thanh toán");
        }

        order.setStatus(OrderStatus.SHIPPED);
        order.setTrackingNumber(trackingNumber);
        order.setShippedAt(ZonedDateTime.now());
        orderCompletionRepository.save(order);

        log.info("Order {} marked as shipped with tracking {}", orderId, trackingNumber);

        // Broadcast status update
        broadcastStatusUpdate(order, "Người bán đã gửi hàng");

        return orderCompletionMapper.toOrderStatusResponse(order);
    }

    /**
     * Buyer confirms receipt
     */
    @Transactional
    public OrderStatusResponse confirmReceipt(Long orderId, Long buyerId) {
        OrderCompletion order = orderCompletionRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order", orderId));

        // Verify buyer
        if (!order.getWinner().getId().equals(buyerId)) {
            throw new UnauthorizedSellerException("Bạn không phải là người mua hàng");
        }

        // Verify status
        if (order.getStatus() != OrderStatus.SHIPPED) {
            throw new BadRequestException("Đơn hàng chưa được gửi đi");
        }

        order.setStatus(OrderStatus.COMPLETED);
        order.setCompletedAt(ZonedDateTime.now());
        orderCompletionRepository.save(order);

        // Transfer money to seller
        stripePaymentService.transferToSeller(order);

        log.info("Order {} completed, buyer confirmed receipt", orderId);

        // Broadcast status update
        broadcastStatusUpdate(order, "Người mua đã xác nhận nhận hàng, giao dịch hoàn tất");

        return orderCompletionMapper.toOrderStatusResponse(order);
    }

    /**
     * Seller cancels order (only before payment)
     * Automatically creates a -1 review for the buyer with comment "Người thắng
     * không thanh toán"
     */
    @Transactional
    public OrderStatusResponse cancelOrder(Long orderId, Long sellerId) {
        OrderCompletion order = orderCompletionRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order", orderId));

        // Verify seller
        if (!order.getProduct().getSeller().getId().equals(sellerId)) {
            throw new UnauthorizedSellerException("Chỉ người bán mới có thể hủy đơn hàng");
        }

        // Verify status - can only cancel if PENDING_PAYMENT
        if (order.getStatus() != OrderStatus.PENDING_PAYMENT) {
            throw new BadRequestException("Không thể hủy đơn hàng đã thanh toán");
        }

        order.setStatus(OrderStatus.CANCELLED);
        orderCompletionRepository.save(order);

        // Automatically create or update -1 review for buyer (winner)
        // Delegates to ProfileService to reuse review logic
        profileService.createOrUpdateReviewInternal(
                order.getProduct().getId(),
                sellerId,
                (short) -1,
                "Người thắng không thanh toán");

        log.info("Order {} cancelled by seller {}", orderId, sellerId);

        // Schedule broadcast AFTER transaction commits
        TransactionSynchronizationManager.registerSynchronization(
                new TransactionSynchronization() {
                    @Override
                    public void afterCommit() {
                        broadcastStatusUpdate(order, "Người bán đã hủy đơn hàng");
                    }
                });

        return orderCompletionMapper.toOrderStatusResponse(order);
    }

    /**
     * Get order status
     */
    @Transactional(readOnly = true)
    public OrderStatusResponse getOrderStatus(Long orderId, Long userId) {
        OrderCompletion order = orderCompletionRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order", orderId));

        // Verify access
        boolean isBuyer = order.getWinner().getId().equals(userId);
        boolean isSeller = order.getProduct().getSeller().getId().equals(userId);

        if (!isBuyer && !isSeller) {
            throw new UnauthorizedSellerException("Bạn không có quyền xem đơn hàng này");
        }

        return orderCompletionMapper.toOrderStatusResponse(order);
    }

    /**
     * Broadcast order status update via WebSocket
     */
    private void broadcastStatusUpdate(OrderCompletion order, String message) {
        OrderStatusEvent event = OrderStatusEvent.builder()
                .orderId(order.getId())
                .status(order.getStatus().name())
                .message(message)
                .build();

        messagingTemplate.convertAndSend("/topic/order/" + order.getId() + "/status", event);
    }
}
