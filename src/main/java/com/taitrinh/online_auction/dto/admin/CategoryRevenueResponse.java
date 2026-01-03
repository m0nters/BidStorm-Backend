package com.taitrinh.online_auction.dto.admin;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Revenue statistics by category")
public class CategoryRevenueResponse {

    @Schema(description = "Category ID", example = "1")
    private Integer categoryId;

    @Schema(description = "Category name", example = "Electronics")
    private String categoryName;

    @Schema(description = "Total revenue in cents", example = "5000000000")
    private Long totalRevenueCents;

    @Schema(description = "Number of products sold in this category", example = "342")
    private Long productCount;
}
