package com.taitrinh.online_auction.service;

import java.math.BigDecimal;
import java.time.ZonedDateTime;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.taitrinh.online_auction.dto.bid.BidRequest;
import com.taitrinh.online_auction.dto.bid.BidResponse;
import com.taitrinh.online_auction.entity.BidHistory;
import com.taitrinh.online_auction.entity.BlockedBidder;
import com.taitrinh.online_auction.entity.Product;
import com.taitrinh.online_auction.entity.User;
import com.taitrinh.online_auction.exception.InvalidBidAmountException;
import com.taitrinh.online_auction.exception.ProductEndedException;
import com.taitrinh.online_auction.exception.ResourceNotFoundException;
import com.taitrinh.online_auction.exception.UnauthorizedBidException;
import com.taitrinh.online_auction.mapper.BidMapper;
import com.taitrinh.online_auction.repository.BidHistoryRepository;
import com.taitrinh.online_auction.repository.BlockedBidderRepository;
import com.taitrinh.online_auction.repository.ProductRepository;
import com.taitrinh.online_auction.repository.UserRepository;
import com.taitrinh.online_auction.service.email.ProductEmailService;
import com.taitrinh.online_auction.util.NameMaskingUtil;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class BidService {

    private final BidHistoryRepository bidHistoryRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;
    private final BlockedBidderRepository blockedBidderRepository;
    private final BidMapper bidMapper;
    private final BidNotificationService notificationService;
    private final EmailService emailService;
    private final ConfigService configService;
    private final ProductEmailService productEmailService;

    /**
     * Place an automatic bid on a product
     * Implements automatic bidding algorithm as per requirement 6.2
     */
    @Transactional
    public BidResponse placeBid(Long productId, BidRequest request, Long userId) {
        log.info("User {} attempting to bid {} on product {}", userId, request.getMaxBidAmount(), productId);

        // Validate product exists and not ended
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy sản phẩm"));

        if (product.isEnded()) {
            throw new ProductEndedException("Sản phẩm đã kết thúc");
        }

        // Validate user exists and can bid
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy người dùng"));

        // Validate user is not the seller
        if (product.getSeller() != null && userId.equals(product.getSeller().getId())) {
            throw new UnauthorizedBidException("Người bán không thể đấu giá chính sản phẩm của mình");
        }

        // Check if user is blocked from bidding on this product
        if (blockedBidderRepository.existsByProduct_IdAndBidder_Id(productId, userId)) {
            throw new UnauthorizedBidException("Bạn đã bị người bán chặn khỏi sản phẩm này");
        }

        // Validate rating using existing canBid() method
        if (!user.canBid()) {
            long totalRatings = user.getPositiveRating() + user.getNegativeRating();

            if (totalRatings == 0 && !product.getAllowUnratedBidders()) {
                throw new UnauthorizedBidException(
                        "Người bán không cho phép người đấu giá không có đánh giá. Hãy nâng cao đánh giá của bạn!");
            } else if (totalRatings > 0) {
                throw new UnauthorizedBidException(
                        "Cần đạt điểm đánh giá ít nhất 80% để đấu giá");
            }
        }

        // Auto-trigger buy now if bid amount >= buy now price
        if (product.getBuyNowPrice() != null &&
                request.getMaxBidAmount().compareTo(product.getBuyNowPrice()) >= 0) {
            log.info("Bid amount {} >= buy now price {}, auto-triggering buy now",
                    request.getMaxBidAmount(), product.getBuyNowPrice());
            return buyNow(productId, userId);
        }

        // Validate bid amount meets minimum
        BigDecimal suggestedPrice = product.getCurrentPrice().add(product.getPriceStep());
        if (request.getMaxBidAmount().compareTo(suggestedPrice) < 0) {
            throw new InvalidBidAmountException(
                    "Giá đấu giá phải lớn hơn hoặc bằng " + suggestedPrice);
        }

        // Get current highest max bid (if any)
        BidHistory currentHighestBid = bidHistoryRepository
                .findFirstByProductIdOrderByMaxBidAmountDescCreatedAtAsc(productId)
                .orElse(null);

        // Calculate new current price
        BigDecimal newCurrentPrice = calculateNewPrice(
                product,
                currentHighestBid,
                request.getMaxBidAmount());
        log.debug("New current price: {}", newCurrentPrice);

        // Determine new highest bidder
        User newHighestBidder;
        if (currentHighestBid == null ||
                request.getMaxBidAmount().compareTo(currentHighestBid.getMaxBidAmount()) > 0) {
            // New bid wins
            newHighestBidder = user;
        } else {
            // New bid loses - current bidder still highest
            newHighestBidder = currentHighestBid.getBidder();
        }

        // Save bid history
        BidHistory bidHistory = BidHistory.builder()
                .product(product)
                .bidder(user)
                .bidAmount(newCurrentPrice) // Actual bid amount (current price after this bid)
                .maxBidAmount(request.getMaxBidAmount()) // Maximum willing to pay
                .build();

        bidHistory = bidHistoryRepository.save(bidHistory);

        // Update product
        BigDecimal oldPrice = product.getCurrentPrice();
        User previousHighestBidder = product.getHighestBidder();

        product.setCurrentPrice(newCurrentPrice);
        product.setHighestBidder(newHighestBidder);
        product.setBidCount(product.getBidCount() + 1);

        // Auto-extend auction if enabled and within trigger window (Requirement 3.1)
        Integer triggerMin = configService.getAutoExtendTriggerMin();
        Integer extendByMin = configService.getAutoExtendByMin();

        if (product.shouldAutoExtend(triggerMin)) {
            ZonedDateTime oldEndTime = product.getEndTime();
            ZonedDateTime newEndTime = oldEndTime.plusMinutes(extendByMin);
            product.setEndTime(newEndTime);
            log.info("Auto-extended auction for product {}. Old end time: {}, New end time: {}",
                    productId, oldEndTime, newEndTime);
        }

        productRepository.save(product);

        log.info("Bid placed successfully. Product {} price updated: {} -> {}, highest bidder: {}",
                productId, oldPrice, newCurrentPrice, newHighestBidder.getId());

        // Create responses for broadcasting
        boolean isSeller = product.getSeller() != null &&
                userId.equals(product.getSeller().getId());

        // Determine if this bid is the highest bidder
        // If newHighestBidder is the user who just placed the bid, they are the highest
        // bidder
        boolean isHighestBidder = newHighestBidder.getId().equals(user.getId());

        // Public channel - masked names
        BidResponse publicResponse = bidMapper.toResponseWithViewer(bidHistory, null, false);
        publicResponse.setIsHighestBidder(isHighestBidder);
        String maskedHighestBidder = NameMaskingUtil.maskName(newHighestBidder.getFullName());
        notificationService.notifyNewBid(productId, publicResponse, newCurrentPrice, maskedHighestBidder,
                product.getEndTime());

        // Seller channel - unmasked names
        BidResponse sellerResponse = bidMapper.toResponseWithViewer(bidHistory, null, true);
        sellerResponse.setIsHighestBidder(isHighestBidder);
        notificationService.notifyNewBidToSeller(productId, sellerResponse, newCurrentPrice,
                newHighestBidder.getFullName(), product.getEndTime());

        // Send email notifications (async)
        sendBidNotificationEmails(product, user, newCurrentPrice, newHighestBidder,
                previousHighestBidder);

        // Return personalized response to bidder
        BidResponse response = bidMapper.toResponseWithViewer(bidHistory, userId, isSeller);
        response.setIsHighestBidder(isHighestBidder);
        return response;
    }

    /**
     * Get bid history for a product with viewer context
     */
    @Transactional(readOnly = true)
    public List<BidResponse> getBidHistory(Long productId, Long viewerId) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy sản phẩm"));

        boolean isProductSeller = viewerId != null &&
                product.getSeller() != null &&
                viewerId.equals(product.getSeller().getId());

        List<BidHistory> bids = bidHistoryRepository.findByProductIdOrderByCreatedAtDesc(productId);

        // Get highest bid (by maxBidAmount, then earliest createdAt)
        BidHistory highestBid = bidHistoryRepository
                .findFirstByProductIdOrderByMaxBidAmountDescCreatedAtAsc(productId)
                .orElse(null);

        List<BidResponse> responses = bidMapper.toResponseListWithViewer(bids, viewerId, isProductSeller);

        // Mark the highest bidder
        if (highestBid != null) {
            responses.forEach(response -> {
                if (response.getId().equals(highestBid.getId())) {
                    response.setIsHighestBidder(true);
                } else {
                    response.setIsHighestBidder(false);
                }
            });
        } else {
            // No bids yet
            responses.forEach(response -> response.setIsHighestBidder(false));
        }

        return responses;
    }

    /**
     * Seller rejects a bidder - removes all their bids and recalculates winner
     * Requirement 3.3: Từ chối lượt ra giá của bidder
     */
    @Transactional
    public void rejectBidder(Long productId, Long bidderId, Long sellerId) {
        log.info("Seller {} rejecting bidder {} from product {}", sellerId, bidderId, productId);

        // Validate seller owns the product
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy sản phẩm"));

        if (product.getSeller() == null || !sellerId.equals(product.getSeller().getId())) {
            throw new UnauthorizedBidException(
                    "Xác thực danh tính người bán không hợp lệ. Không thể từ chối người đấu giá");
        }

        // Get bidder info before deleting
        User rejectedBidder = userRepository.findById(bidderId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy người đấu giá"));

        // Remove all bids from this bidder
        List<BidHistory> bidderBids = bidHistoryRepository.findByProductIdAndBidderId(productId, bidderId);
        bidHistoryRepository.deleteAll(bidderBids);

        // Add to blocked bidders table to prevent future bids
        if (!blockedBidderRepository.existsByProduct_IdAndBidder_Id(productId, bidderId)) {
            BlockedBidder blockedBidder = BlockedBidder.builder()
                    .product(product)
                    .bidder(rejectedBidder)
                    .build();
            blockedBidderRepository.save(blockedBidder);
            log.info("Added bidder {} to blocked list for product {}", bidderId, productId);
        }

        // Recalculate highest bidder
        BidHistory newHighestBid = bidHistoryRepository
                .findFirstByProductIdOrderByMaxBidAmountDescCreatedAtAsc(productId)
                .orElse(null);

        if (newHighestBid != null) {
            // Update product with new highest bidder
            product.setHighestBidder(newHighestBid.getBidder());
            product.setCurrentPrice(newHighestBid.getMaxBidAmount());
        } else {
            // No more bids - reset to starting price
            product.setHighestBidder(null);
            product.setCurrentPrice(product.getStartingPrice());
        }

        product.setBidCount(Math.max(0, product.getBidCount() - bidderBids.size()));
        productRepository.save(product);

        // Prepare data for WebSocket broadcast
        BigDecimal broadcastPrice = (newHighestBid != null)
                ? newHighestBid.getMaxBidAmount()
                : product.getStartingPrice();
        String broadcastBidder = (newHighestBid != null)
                ? NameMaskingUtil.maskName(newHighestBid.getBidder().getFullName())
                : null;

        // Notify via WebSocket with updated auction state
        notificationService.notifyBidRejected(productId, bidderId, broadcastPrice, broadcastBidder);

        // Send rejection email
        emailService.sendBidRejectionEmail(rejectedBidder.getEmail(), rejectedBidder.getFullName(), product.getTitle(),
                product.getSlug());

        log.info("Successfully rejected bidder {} from product {}", bidderId, productId);
    }

    /**
     * Calculate new product price based on automatic bidding algorithm
     */
    private BigDecimal calculateNewPrice(Product product, BidHistory currentHighestBid,
            BigDecimal newMaxBid) {
        if (currentHighestBid == null) {
            // First bid - start at current price (starting price)
            return product.getCurrentPrice();
        }

        BigDecimal currentMaxBid = currentHighestBid.getMaxBidAmount();

        if (newMaxBid.compareTo(currentMaxBid) <= 0 && newMaxBid.compareTo(product.getCurrentPrice()) > 0) {
            return newMaxBid;
        }

        if (newMaxBid.compareTo(currentMaxBid) > 0) {
            return currentMaxBid.add(product.getPriceStep());
        }

        return currentMaxBid;
    }

    /**
     * Send email notifications for bid event
     */
    private void sendBidNotificationEmails(Product product, User newBidder,
            BigDecimal newPrice, User newHighestBidder, User previousHighestBidder) {
        // Email to seller
        if (product.getSeller() != null) {
            emailService.sendNewBidNotificationToSeller(
                    product.getSeller().getEmail(),
                    product.getSeller().getFullName(),
                    product.getTitle(),
                    newBidder.getFullName(),
                    newPrice,
                    product.getSlug() // Product slug for link
            );
        }

        // Email to new bidder (confirmation)
        emailService.sendBidConfirmationToBidder(
                newBidder.getEmail(),
                newBidder.getFullName(),
                product.getTitle(),
                newPrice,
                newBidder.getId().equals(newHighestBidder.getId()), // Are they winning?
                product.getSlug() // Product slug for link
        );

        // Email to previous highest bidder (outbid notification)
        if (previousHighestBidder != null &&
                !previousHighestBidder.getId().equals(newHighestBidder.getId()) &&
                !previousHighestBidder.getId().equals(newBidder.getId())) {
            emailService.sendOutbidNotification(
                    previousHighestBidder.getEmail(),
                    previousHighestBidder.getFullName(),
                    product.getTitle(),
                    newPrice,
                    product.getSlug() // Product slug for link
            );
        }
    }

    /**
     * Buy now - immediately purchase product at buy now price
     * Ends the auction and sets buyer as winner
     */
    @Transactional
    public BidResponse buyNow(Long productId, Long userId) {
        log.info("User {} attempting to buy now product {}", userId, productId);

        // Validate product exists and not ended
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy sản phẩm"));

        if (product.isEnded()) {
            throw new ProductEndedException("Sản phẩm đã kết thúc");
        }

        // Validate buy now price exists
        if (product.getBuyNowPrice() == null) {
            throw new InvalidBidAmountException("Sản phẩm không có giá mua ngay");
        }

        // Validate user exists
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy người dùng"));

        // Validate user is not the seller
        if (product.getSeller() != null && userId.equals(product.getSeller().getId())) {
            throw new UnauthorizedBidException("Người bán không thể mua chính sản phẩm của mình");
        }

        // Check if user is blocked from bidding on this product
        if (blockedBidderRepository.existsByProduct_IdAndBidder_Id(productId, userId)) {
            throw new UnauthorizedBidException("Bạn đã bị người bán chặn khỏi sản phẩm này");
        }

        // Create bid history record with buy now price
        BidHistory bidHistory = BidHistory.builder()
                .product(product)
                .bidder(user)
                .bidAmount(product.getBuyNowPrice())
                .maxBidAmount(product.getBuyNowPrice())
                .build();

        bidHistory = bidHistoryRepository.save(bidHistory);

        // Update product - set winner, highest bidder, mark as ended
        User previousHighestBidder = product.getHighestBidder();

        product.setCurrentPrice(product.getBuyNowPrice());
        product.setHighestBidder(user);
        product.setWinner(user);
        product.setIsEnded(true);
        product.setBidCount(product.getBidCount() + 1);
        productRepository.save(product);

        log.info("Product {} bought now by user {} at price {}", productId, userId, product.getBuyNowPrice());

        // Create response for buyer (personalized)
        BidResponse response = bidMapper.toResponseWithViewer(bidHistory, userId, false);
        response.setIsHighestBidder(true);

        // PUBLIC CHANNEL - masked BidResponse + masked name
        BidResponse publicResponse = bidMapper.toResponseWithViewer(bidHistory, null, false);
        publicResponse.setIsHighestBidder(true);
        String maskedWinnerName = NameMaskingUtil.maskName(user.getFullName());

        // SELLER CHANNEL - unmasked BidResponse + unmasked name
        BidResponse sellerResponse = bidMapper.toResponseWithViewer(bidHistory, null, true);
        sellerResponse.setIsHighestBidder(true);
        String unmaskedWinnerName = user.getFullName();

        // Broadcast to both channels
        notificationService.notifyProductBoughtNowToPublic(productId, publicResponse, product.getBuyNowPrice(),
                maskedWinnerName);
        notificationService.notifyProductBoughtNowToSeller(productId, sellerResponse, product.getBuyNowPrice(),
                unmaskedWinnerName);

        // Send email notifications
        sendBuyNowNotificationEmails(product, user, previousHighestBidder);

        return response;
    }

    /**
     * Send email notifications for buy now event
     */
    private void sendBuyNowNotificationEmails(Product product, User winner, User previousHighestBidder) {
        // Email to seller
        if (product.getSeller() != null) {
            productEmailService.sendWinnerNotificationToSeller(
                    product.getSeller().getEmail(),
                    product.getSeller().getFullName(),
                    product.getTitle(),
                    winner.getFullName(),
                    product.getBuyNowPrice(),
                    product.getSlug());
        }

        // Email to winner
        productEmailService.sendWinnerNotificationToBidder(
                winner.getEmail(),
                winner.getFullName(),
                product.getTitle(),
                product.getBuyNowPrice(),
                product.getSlug());

        // Email to previous highest bidder if exists
        if (previousHighestBidder != null && !previousHighestBidder.getId().equals(winner.getId())) {
            emailService.sendOutbidNotification(
                    previousHighestBidder.getEmail(),
                    previousHighestBidder.getFullName(),
                    product.getTitle(),
                    product.getBuyNowPrice(),
                    product.getSlug());
        }
    }
}
