package com.taitrinh.online_auction.dto.websocket;

import java.math.BigDecimal;
import java.time.ZonedDateTime;

import com.taitrinh.online_auction.dto.bid.BidResponse;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BidEvent {

    public enum EventType {
        NEW_BID,
        BID_REJECTED,
        PRODUCT_BOUGHT_NOW
    }

    private EventType type;
    private Long productId;
    private BidResponse bid;
    private BigDecimal currentPrice;
    private String highestBidder;
    private ZonedDateTime endTime;
    private Boolean isEnded;

    // Factory methods for easy event creation
    public static BidEvent newBid(Long productId, BidResponse bid, BigDecimal currentPrice, String highestBidder,
            ZonedDateTime endTime) {
        return BidEvent.builder()
                .type(EventType.NEW_BID)
                .productId(productId)
                .bid(bid)
                .currentPrice(currentPrice)
                .highestBidder(highestBidder)
                .endTime(endTime)
                .isEnded(false)
                .build();
    }

    public static BidEvent bidRejected(Long productId, Long bidderId) {
        return BidEvent.builder()
                .type(EventType.BID_REJECTED)
                .productId(productId)
                .bid(BidResponse.builder().bidderId(bidderId).build())
                .build();
    }

    public static BidEvent productBoughtNow(Long productId, BidResponse bid, BigDecimal finalPrice, String winnerName) {
        return BidEvent.builder()
                .type(EventType.PRODUCT_BOUGHT_NOW)
                .productId(productId)
                .bid(bid)
                .currentPrice(finalPrice)
                .highestBidder(winnerName)
                .isEnded(true)
                .build();
    }
}
