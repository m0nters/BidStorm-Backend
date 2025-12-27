package com.taitrinh.online_auction.dto.product;

import java.math.BigDecimal;
import java.time.ZonedDateTime;

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
@Schema(description = "Response after creating a product")
public class CreateProductResponse {

    @Schema(description = "Product ID", example = "1")
    private Long id;

    @Schema(description = "Product title", example = "iPhone 15 Pro Max 256GB")
    private String title;

    @Schema(description = "Product slug (URL-friendly identifier)", example = "iphone-15-pro-max-256gb")
    private String slug;

    @Schema(description = "Category ID", example = "5")
    private Integer categoryId;

    @Schema(description = "Category name", example = "Điện thoại di động")
    private String categoryName;

    @Schema(description = "Seller ID", example = "10")
    private Long sellerId;

    @Schema(description = "Seller name", example = "John Doe")
    private String sellerName;

    @Schema(description = "Starting price", example = "20000000")
    private BigDecimal startingPrice;

    @Schema(description = "Current price (initially same as starting price)", example = "20000000")
    private BigDecimal currentPrice;

    @Schema(description = "Buy now price (if available)", example = "30000000")
    private BigDecimal buyNowPrice;

    @Schema(description = "Price step", example = "100000")
    private BigDecimal priceStep;

    @Schema(description = "Auto-extend enabled", example = "true")
    @JsonProperty("isAutoExtend")
    private Boolean isAutoExtend;

    @Schema(description = "Auction start time", example = "2025-12-03T10:00:00Z")
    private ZonedDateTime startTime;

    @Schema(description = "Auction end time", example = "2025-12-10T10:00:00Z")
    private ZonedDateTime endTime;

    @Schema(description = "Product creation time", example = "2025-12-03T10:00:00Z")
    private ZonedDateTime createdAt;

    @Schema(description = "Number of images uploaded", example = "3")
    private Integer imageCount;
}
