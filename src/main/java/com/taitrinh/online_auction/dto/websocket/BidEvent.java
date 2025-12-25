package com.taitrinh.online_auction.dto.websocket;

import java.math.BigDecimal;

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
        BID_REJECTED
    }

    private EventType type;
    private Long productId;
    private BidResponse bid;
    private BigDecimal currentPrice;
    private String highestBidder;

    // Factory methods for easy event creation
    public static BidEvent newBid(Long productId, BidResponse bid, BigDecimal currentPrice, String highestBidder) {
        return BidEvent.builder()
                .type(EventType.NEW_BID)
                .productId(productId)
                .bid(bid)
                .currentPrice(currentPrice)
                .highestBidder(highestBidder)
                .build();
    }

    public static BidEvent bidRejected(Long productId, Long bidderId) {
        return BidEvent.builder()
                .type(EventType.BID_REJECTED)
                .productId(productId)
                .bid(BidResponse.builder().bidderId(bidderId).build())
                .build();
    }
}
