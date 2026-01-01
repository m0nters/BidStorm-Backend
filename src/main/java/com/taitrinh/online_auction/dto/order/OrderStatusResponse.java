package com.taitrinh.online_auction.dto.order;

import java.time.ZonedDateTime;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderStatusResponse {

    private Long id;
    private Long productId;
    private String productTitle;
    private Long winnerId;
    private Long sellerId;
    private String status;
    private String shippingAddress;
    private String shippingPhone;
    private String trackingNumber;
    private Long amountCents;
    private String currency;
    private ZonedDateTime paidAt;
    private ZonedDateTime shippedAt;
    private ZonedDateTime completedAt;
    private ZonedDateTime createdAt;
}
