package com.taitrinh.online_auction.dto.profile;

import java.math.BigDecimal;
import java.time.ZonedDateTime;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Product that user is currently bidding on")
public class BiddingProductResponse {

    @Schema(description = "Product ID", example = "1")
    private Long productId;

    @Schema(description = "Product title", example = "iPhone 13 Pro Max")
    private String title;

    @Schema(description = "Product slug", example = "iphone-13-pro-max")
    private String slug;

    @Schema(description = "Main thumbnail image URL", example = "/uploads/products/image.jpg")
    private String thumbnailUrl;

    @Schema(description = "Current highest price", example = "15000000")
    private BigDecimal currentPrice;

    @Schema(description = "User's highest bid amount", example = "14500000")
    private BigDecimal userHighestBid;

    @Schema(description = "Whether user is currently winning", example = "false")
    private Boolean isWinning;

    @Schema(description = "Total bid count", example = "15")
    private Integer bidCount;

    @Schema(description = "Auction end time")
    private ZonedDateTime endTime;

    @Schema(description = "Whether auction has ended", example = "false")
    private Boolean isEnded;
}
