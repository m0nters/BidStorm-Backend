package com.taitrinh.online_auction.dto.product;

import java.math.BigDecimal;
import java.time.ZonedDateTime;
import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Request to create a new auction product")
public class CreateProductRequest {

    @NotNull(message = "Category ID is required")
    @Schema(description = "Category ID", example = "5")
    private Integer categoryId;

    @NotBlank(message = "Product title is required")
    @Size(min = 10, max = 255, message = "Title must be between 10 and 255 characters")
    @Schema(description = "Product title", example = "iPhone 15 Pro Max 256GB - Brand New")
    private String title;

    @NotBlank(message = "Product description is required")
    @Size(min = 50, message = "Description must be at least 50 characters")
    @Schema(description = "Product description (HTML supported)", example = "<p>Brand new iPhone 15 Pro Max with 256GB storage...</p>")
    private String description;

    @NotNull(message = "Starting price is required")
    @DecimalMin(value = "0.01", message = "Starting price must be greater than 0")
    @Schema(description = "Starting price (VND)", example = "20000000")
    private BigDecimal startingPrice;

    @NotNull(message = "Price step is required")
    @DecimalMin(value = "0.01", message = "Price step must be greater than 0")
    @Schema(description = "Bid increment step (VND)", example = "100000")
    private BigDecimal priceStep;

    @DecimalMin(value = "0.01", message = "Buy now price must be greater than 0")
    @Schema(description = "Buy now price (optional, VND)", example = "30000000")
    private BigDecimal buyNowPrice;

    @NotNull(message = "End time is required")
    @Future(message = "End time must be in the future")
    @Schema(description = "Auction end time", example = "2025-12-15T23:59:59+07:00")
    private ZonedDateTime endTime;

    @NotNull(message = "Auto-extend setting is required")
    @Schema(description = "Enable auto-extend when bid placed near end time", example = "true")
    private Boolean autoExtend;

    @NotEmpty(message = "At least 3 images are required")
    @Size(min = 3, message = "At least 3 images are required")
    @Valid
    @Schema(description = "Product images (minimum 3)")
    private List<ProductImageRequest> images;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "Product image information")
    public static class ProductImageRequest {

        @NotBlank(message = "Image URL is required")
        @Schema(description = "Image URL", example = "https://example.com/images/product1.jpg")
        private String imageUrl;

        @NotNull(message = "isPrimary flag is required")
        @Schema(description = "Is this the primary/thumbnail image", example = "true")
        private Boolean isPrimary;

        @NotNull(message = "Sort order is required")
        @Positive(message = "Sort order must be positive")
        @Schema(description = "Display order (1, 2, 3, ...)", example = "1")
        private Integer sortOrder;
    }
}
