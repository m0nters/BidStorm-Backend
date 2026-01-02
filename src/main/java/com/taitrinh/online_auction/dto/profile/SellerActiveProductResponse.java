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
@Schema(description = "Seller's active product information")
public class SellerActiveProductResponse {

    @Schema(description = "Product ID", example = "1")
    private Long id;

    @Schema(description = "Product title", example = "iPhone 15 Pro Max")
    private String title;

    @Schema(description = "Product URL slug", example = "iphone-15-pro-max-256gb")
    private String slug;

    @Schema(description = "Product thumbnail URL", example = "https://example.com/image.jpg")
    private String thumbnailUrl;

    @Schema(description = "Starting price", example = "10000000")
    private BigDecimal startingPrice;

    @Schema(description = "Current price", example = "15000000")
    private BigDecimal currentPrice;

    @Schema(description = "Buy now price (if available)", example = "25000000")
    private BigDecimal buyNowPrice;

    @Schema(description = "Number of bids", example = "10")
    private Integer bidCount;

    @Schema(description = "Auction end time")
    private ZonedDateTime endTime;

    @Schema(description = "Product created time")
    private ZonedDateTime createdAt;

    @Schema(description = "Category name", example = "Electronics")
    private String categoryName;

    @Schema(description = "Category slug", example = "electronics")
    private String categorySlug;
}
