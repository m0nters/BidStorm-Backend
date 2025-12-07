package com.taitrinh.online_auction.dto.category;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Request to create a new category")
public class CreateCategoryRequest {

    @NotBlank(message = "Category name is required")
    @Size(min = 2, max = 255, message = "Category name must be between 2 and 255 characters")
    @Schema(description = "Category name", example = "Điện tử")
    private String name;

    @Size(max = 500, message = "Slug must not exceed 500 characters")
    @Schema(description = "Custom slug (optional, auto-generated from name if not provided)", example = "dien-tu-custom")
    private String slug;

    @Schema(description = "Parent category ID for sub-category (null for top-level category)", example = "1")
    private Integer parentId;
}
