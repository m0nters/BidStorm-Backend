package com.taitrinh.online_auction.dto.product;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Product search and filter request")
public class ProductSearchRequest {

    @Schema(description = "Search keyword", example = "iPhone")
    private String keyword;

    @Schema(description = "Category ID to filter by", example = "5")
    private Integer categoryId;

    @Schema(description = "Page number (0-indexed)", example = "0")
    @Min(value = 0, message = "Page must be >= 0")
    @Builder.Default
    private Integer page = 0;

    @Schema(description = "Page size", example = "20")
    @Min(value = 1, message = "Size must be >= 1")
    @Builder.Default
    private Integer size = 20;

    @Schema(description = "Sort field", example = "endTime", allowableValues = { "endTime", "currentPrice", "createdAt",
            "bidCount" })
    @Pattern(regexp = "^(endTime|currentPrice|createdAt|bidCount)$", message = "Invalid sort field")
    @Builder.Default
    private String sortBy = "endTime";

    @Schema(description = "Sort direction", example = "asc", allowableValues = { "asc", "desc" })
    @Pattern(regexp = "^(asc|desc)$", message = "Invalid sort direction")
    @Builder.Default
    private String sortDirection = "asc";
}
