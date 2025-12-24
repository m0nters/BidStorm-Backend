package com.taitrinh.online_auction.dto.product;

import java.math.BigDecimal;
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
@Schema(description = "Product list item response (for display in lists, grids, etc. with less detailed information)")
public class ProductListResponse {

    @Schema(description = "Product ID", example = "1")
    private Long id;

    @Schema(description = "Product title", example = "iPhone 15 Pro Max 256GB")
    private String title;

    @Schema(description = "Product slug", example = "iphone-15-pro-max-256gb")
    private String slug;

    @Schema(description = "Main thumbnail image URL", example = "https://example.com/images/product1.jpg")
    private String thumbnailUrl;

    @Schema(description = "Current price", example = "25000000")
    private BigDecimal currentPrice;

    @Schema(description = "Buy now price (if available)", example = "30000000")
    private BigDecimal buyNowPrice;

    @Schema(description = "Allow unrated bidders to participate", example = "false")
    private Boolean allowUnratedBidders;

    @Schema(description = "Auto extend auction", example = "true")
    private boolean autoExtend;

    @Schema(description = "Category ID", example = "5")
    private Integer categoryId;

    @Schema(description = "Category name", example = "Điện thoại di động")
    private String categoryName;

    @Schema(description = "Category slug", example = "dien-thoai-di-dong")
    private String categorySlug;

    @Schema(description = "Seller ID", example = "10")
    private Long sellerId;

    @Schema(description = "Seller name", example = "John Doe")
    private String sellerName;

    @Schema(description = "Seller rating percentage", example = "85.5")
    private Double sellerRating;

    @Schema(description = "Highest bidder ID (if any)", example = "15")
    private Long highestBidderId;

    @Schema(description = "Highest bidder name (masked)", example = "****Khoa")
    private String highestBidderName;

    @Schema(description = "Highest bidder rating percentage", example = "90.0")
    private Double highestBidderRating;

    @Schema(description = "Number of bids", example = "25")
    private Integer bidCount;

    @Schema(description = "Product creation time", example = "2025-12-01T10:00:00Z")
    private ZonedDateTime createdAt;

    @Schema(description = "Auction end time", example = "2025-12-10T10:00:00Z")
    private ZonedDateTime endTime;

    @Schema(description = "Whether product is newly posted (within N minutes)", example = "true")
    private Boolean isNew;
}
