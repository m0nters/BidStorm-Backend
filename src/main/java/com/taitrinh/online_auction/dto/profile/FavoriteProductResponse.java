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
@Schema(description = "Favorite product with additional metadata")
public class FavoriteProductResponse {

    @Schema(description = "Product ID", example = "1")
    private Long productId;

    @Schema(description = "Product title", example = "iPhone 13 Pro Max")
    private String title;

    @Schema(description = "Product slug", example = "iphone-13-pro-max")
    private String slug;

    @Schema(description = "Main thumbnail image URL", example = "/uploads/products/image.jpg")
    private String thumbnailUrl;

    @Schema(description = "Current price", example = "15000000")
    private BigDecimal currentPrice;

    @Schema(description = "Buy now price (if available)", example = "18000000")
    private BigDecimal buyNowPrice;

    @Schema(description = "Number of bids", example = "25")
    private Integer bidCount;

    @Schema(description = "Auction end time")
    private ZonedDateTime endTime;

    @Schema(description = "Whether auction has ended", example = "false")
    private Boolean isEnded;

    @Schema(description = "When the product was added to favorites")
    private ZonedDateTime favoritedAt;
}
