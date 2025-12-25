package com.taitrinh.online_auction.dto.bid;

import java.math.BigDecimal;
import java.time.ZonedDateTime;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BidResponse {

    private Long id;
    private Long productId;
    private Long bidderId;
    private String bidderName; // Masked for privacy (****Khoa), unmasked for seller/own bid
    private BigDecimal bidAmount; // Actual bid amount (current price) - visible to everyone
    private BigDecimal maxBidAmount; // Maximum bid - only shown for own bids or to seller
    private ZonedDateTime createdAt;

    // Helper flags for frontend
    private Boolean isYourself; // True if viewer is the bidder
    private Boolean isHighestBidder; // True if this is the highest bid
}
