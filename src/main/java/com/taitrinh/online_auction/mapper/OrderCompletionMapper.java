package com.taitrinh.online_auction.mapper;

import org.springframework.stereotype.Component;

import com.taitrinh.online_auction.dto.order.OrderStatusResponse;
import com.taitrinh.online_auction.entity.OrderCompletion;

@Component
public class OrderCompletionMapper {

    /**
     * Map OrderCompletion entity to OrderStatusResponse DTO
     */
    public OrderStatusResponse toOrderStatusResponse(OrderCompletion order) {
        if (order == null) {
            return null;
        }

        return OrderStatusResponse.builder()
                .id(order.getId())
                .productId(order.getProduct().getId())
                .productTitle(order.getProduct().getTitle())
                .winnerId(order.getWinner().getId())
                .sellerId(order.getProduct().getSeller().getId())
                .status(order.getStatus().name())
                .shippingAddress(order.getShippingAddress())
                .shippingPhone(order.getShippingPhone())
                .trackingNumber(order.getTrackingNumber())
                .amountCents(order.getAmountCents())
                .currency(order.getCurrency())
                .paidAt(order.getPaidAt())
                .shippedAt(order.getShippedAt())
                .completedAt(order.getCompletedAt())
                .createdAt(order.getCreatedAt())
                .build();
    }
}
