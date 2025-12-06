package com.taitrinh.online_auction.dto.product;

import java.math.BigDecimal;
import java.time.ZonedDateTime;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Detailed product information")
public class ProductDetailResponse {

    @Schema(description = "Product ID", example = "1")
    private Long id;

    @Schema(description = "Product title", example = "iPhone 15 Pro Max 256GB")
    private String title;

    @Schema(description = "Product description (full HTML content)", example = "<p>Brand new iPhone...</p>")
    private String description;

    @Schema(description = "All product images (first one is thumbnail)")
    private List<ProductImageResponse> images;

    @Schema(description = "Starting price", example = "20000000")
    private BigDecimal startingPrice;

    @Schema(description = "Current price", example = "25000000")
    private BigDecimal currentPrice;

    @Schema(description = "Buy now price (if available)", example = "30000000")
    private BigDecimal buyNowPrice;

    @Schema(description = "Price step", example = "100000")
    private BigDecimal priceStep;

    @Schema(description = "Auto-extend enabled", example = "true")
    @JsonProperty("isAutoExtend")
    private Boolean isAutoExtend;

    @Schema(description = "Category ID", example = "5")
    private Integer categoryId;

    @Schema(description = "Category name", example = "Điện thoại di động")
    private String categoryName;

    @Schema(description = "Parent category name (if exists)", example = "Điện tử")
    private String parentCategoryName;

    @Schema(description = "Seller information")
    private UserBasicInfo seller;

    @Schema(description = "Highest bidder information (if any)")
    private UserBasicInfo highestBidder;

    @Schema(description = "Winner information (if auction ended)")
    private UserBasicInfo winner;

    @Schema(description = "Number of bids", example = "25")
    private Integer bidCount;

    @Schema(description = "Number of views", example = "150")
    private Integer viewCount;

    @Schema(description = "Auction start time", example = "2025-12-01T10:00:00Z")
    private ZonedDateTime startTime;

    @Schema(description = "Auction end time", example = "2025-12-10T10:00:00Z")
    private ZonedDateTime endTime;

    @Schema(description = "Product creation time", example = "2025-12-01T09:00:00Z")
    private ZonedDateTime createdAt;

    @Schema(description = "Last update time", example = "2025-12-01T10:00:00Z")
    private ZonedDateTime updatedAt;

    @Schema(description = "Whether product is ended", example = "false")
    @JsonProperty("isEnded")
    private Boolean isEnded;

    @Schema(description = "Whether product is newly posted", example = "true")
    @JsonProperty("isNew")
    private Boolean isNew;

    @Schema(description = "Whether product has buy now option", example = "true")
    @JsonProperty("hasBuyNow")
    private Boolean hasBuyNow;

    @Schema(description = "Description update logs")
    private List<DescriptionLogResponse> descriptionLogs;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "Basic user information")
    public static class UserBasicInfo {
        @Schema(description = "User ID", example = "10")
        private Long id;

        @Schema(description = "User full name", example = "John Doe")
        private String fullName;

        @Schema(description = "Positive rating count", example = "50")
        private Integer positiveRating;

        @Schema(description = "Negative rating count", example = "5")
        private Integer negativeRating;

        @Schema(description = "Rating percentage", example = "90.91")
        private Double ratingPercentage;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "Product image information")
    public static class ProductImageResponse {
        @Schema(description = "Image ID", example = "1")
        private Long id;

        @Schema(description = "Image URL", example = "https://example.com/images/product1.jpg")
        private String imageUrl;

        @Schema(description = "Image order", example = "1")
        private Integer displayOrder;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "Description update log")
    public static class DescriptionLogResponse {
        @Schema(description = "Log ID", example = "1")
        private Long id;

        @Schema(description = "Updated content", example = "Added warranty information")
        private String updatedContent;

        @Schema(description = "Update time", example = "2025-12-02T10:00:00Z")
        private ZonedDateTime updatedAt;
    }
}
