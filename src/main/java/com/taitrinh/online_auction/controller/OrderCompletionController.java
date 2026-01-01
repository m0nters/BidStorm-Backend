package com.taitrinh.online_auction.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.taitrinh.online_auction.dto.ApiResponse;
import com.taitrinh.online_auction.dto.order.ConfirmShipmentRequest;
import com.taitrinh.online_auction.dto.order.InitiatePaymentRequest;
import com.taitrinh.online_auction.dto.order.OrderStatusResponse;
import com.taitrinh.online_auction.dto.order.PaymentIntentResponse;
import com.taitrinh.online_auction.security.UserDetailsImpl;
import com.taitrinh.online_auction.service.OrderCompletionService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/api/v1/orders")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Order Completion", description = "APIs for order management and payment flow")
public class OrderCompletionController {

    private final OrderCompletionService orderCompletionService;

    @PostMapping("/{productId}/create")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Create order", description = "Create order for a product after auction ends (seller or winner can create)")
    public ResponseEntity<ApiResponse<OrderStatusResponse>> createOrder(
            @PathVariable Long productId,
            @AuthenticationPrincipal UserDetailsImpl userDetails) {

        OrderStatusResponse response = orderCompletionService.createOrder(productId, userDetails.getUserId());

        return ResponseEntity.ok(ApiResponse.ok(response, "Tạo đơn hàng thành công"));
    }

    @GetMapping("/{orderId}")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Get order status", description = "Get current order details and status")
    public ResponseEntity<ApiResponse<OrderStatusResponse>> getOrderStatus(
            @PathVariable Long orderId,
            @AuthenticationPrincipal UserDetailsImpl userDetails) {

        OrderStatusResponse response = orderCompletionService.getOrderStatus(orderId, userDetails.getUserId());

        return ResponseEntity.ok(ApiResponse.ok(response, "Lấy thông tin đơn hàng thành công"));
    }

    @PostMapping("/{orderId}/payment")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Initiate payment", description = "Create Stripe PaymentIntent for buyer to pay")
    public ResponseEntity<ApiResponse<PaymentIntentResponse>> initiatePayment(
            @PathVariable Long orderId,
            @Valid @RequestBody InitiatePaymentRequest request,
            @AuthenticationPrincipal UserDetailsImpl userDetails) {

        PaymentIntentResponse response = orderCompletionService.initiatePayment(
                orderId,
                request.getShippingAddress(),
                request.getShippingPhone(),
                userDetails.getUserId());

        return ResponseEntity.ok(ApiResponse.ok(response, "Khởi tạo thanh toán thành công"));
    }

    @PostMapping("/{orderId}/ship")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Mark as shipped", description = "Seller confirms product has been shipped")
    public ResponseEntity<ApiResponse<OrderStatusResponse>> markAsShipped(
            @PathVariable Long orderId,
            @Valid @RequestBody ConfirmShipmentRequest request,
            @AuthenticationPrincipal UserDetailsImpl userDetails) {

        OrderStatusResponse response = orderCompletionService.markAsShipped(
                orderId,
                request.getTrackingNumber(),
                userDetails.getUserId());

        return ResponseEntity.ok(ApiResponse.ok(response, "Đánh dấu đã gửi hàng thành công"));
    }

    @PostMapping("/{orderId}/confirm")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Confirm receipt", description = "Buyer confirms received product")
    public ResponseEntity<ApiResponse<OrderStatusResponse>> confirmReceipt(
            @PathVariable Long orderId,
            @AuthenticationPrincipal UserDetailsImpl userDetails) {

        OrderStatusResponse response = orderCompletionService.confirmReceipt(orderId, userDetails.getUserId());

        return ResponseEntity.ok(ApiResponse.ok(response, "Xác nhận nhận hàng thành công"));
    }

    @PostMapping("/{orderId}/cancel")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Cancel order", description = "Seller cancels order (only before payment)")
    public ResponseEntity<ApiResponse<OrderStatusResponse>> cancelOrder(
            @PathVariable Long orderId,
            @AuthenticationPrincipal UserDetailsImpl userDetails) {

        OrderStatusResponse response = orderCompletionService.cancelOrder(orderId, userDetails.getUserId());

        return ResponseEntity.ok(ApiResponse.ok(response, "Hủy đơn hàng thành công"));
    }
}
