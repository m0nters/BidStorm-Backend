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
@Schema(description = "Product that user has won")
public class WonProductResponse {

    @Schema(description = "Product ID", example = "1")
    private Long productId;

    @Schema(description = "Product title", example = "iPhone 13 Pro Max")
    private String title;

    @Schema(description = "Product slug", example = "iphone-13-pro-max")
    private String slug;

    @Schema(description = "Main product image URL", example = "/uploads/products/image.jpg")
    private String mainImage;

    @Schema(description = "Winning bid amount", example = "15000000")
    private BigDecimal winningBid;

    @Schema(description = "Seller ID", example = "5")
    private Long sellerId;

    @Schema(description = "Seller name", example = "Jane Smith")
    private String sellerName;

    @Schema(description = "Auction end time")
    private ZonedDateTime endTime;

    @Schema(description = "Whether user has reviewed the seller", example = "false")
    private Boolean hasReviewed;
}
