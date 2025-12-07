package com.taitrinh.online_auction.dto.category;

import java.time.ZonedDateTime;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Category response with optional children")
public class CategoryResponse {

    @Schema(description = "Category ID", example = "1")
    private Integer id;

    @Schema(description = "Category name", example = "Điện tử")
    private String name;

    @Schema(description = "Category slug", example = "dien-tu")
    private String slug;

    @Schema(description = "Parent category ID (null if top-level)", example = "null")
    private Integer parentId;

    @Schema(description = "Sub-categories (only included in hierarchy view)")
    private List<CategoryResponse> children;

    @Schema(description = "Creation timestamp", example = "2025-12-02T10:30:00+07:00")
    private ZonedDateTime createdAt;

    @Schema(description = "Whether this is a parent category", example = "true")
    private Boolean isParent;

    @Schema(description = "Number of sub-categories", example = "5")
    private Integer childrenCount;
}
