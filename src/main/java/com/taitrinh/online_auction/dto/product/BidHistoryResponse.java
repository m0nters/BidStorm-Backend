package com.taitrinh.online_auction.dto.product;

import java.time.ZonedDateTime;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Bid history entry (masked bidder info)")
public class BidHistoryResponse {

    @Schema(description = "Bid ID", example = "1")
    private Long id;

    @Schema(description = "Bidder name (masked)", example = "****Khoa")
    private String bidderName;

    @Schema(description = "Bid amount", example = "25000000")
    private String bidAmount;

    @Schema(description = "Bid time", example = "2025-12-01T10:00:00Z")
    private ZonedDateTime bidTime;
}
