package com.taitrinh.online_auction.dto.product;

import java.math.BigDecimal;
import java.time.ZonedDateTime;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request DTO for creating a product with file uploads
 * This is used with multipart/form-data requests
 * Image files are sent separately as MultipartFile[]
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateProductWithFilesRequest {

    @NotNull(message = "Category ID is required")
    private Integer categoryId;

    @NotBlank(message = "Product title is required")
    @Size(min = 10, max = 255, message = "Title must be between 10 and 255 characters")
    private String title;

    @NotBlank(message = "Product description is required")
    @Size(min = 50, message = "Description must be at least 50 characters")
    private String description;

    @NotNull(message = "Starting price is required")
    @DecimalMin(value = "0.01", message = "Starting price must be greater than 0")
    private BigDecimal startingPrice;

    @NotNull(message = "Price step is required")
    @DecimalMin(value = "0.01", message = "Price step must be greater than 0")
    private BigDecimal priceStep;

    @DecimalMin(value = "0.01", message = "Buy now price must be greater than 0")
    private BigDecimal buyNowPrice;

    @NotNull(message = "End time is required")
    @Future(message = "End time must be in the future")
    private ZonedDateTime endTime;

    @NotNull(message = "Auto-extend setting is required")
    private Boolean autoExtend;

    @NotNull(message = "Allow unrated bidders setting is required")
    private Boolean allowUnratedBidders;
}
