package com.taitrinh.online_auction.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.taitrinh.online_auction.dto.ApiResponse;
import com.taitrinh.online_auction.dto.category.CategoryResponse;
import com.taitrinh.online_auction.dto.category.CreateCategoryRequest;
import com.taitrinh.online_auction.service.CategoryService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/categories")
@RequiredArgsConstructor
@Tag(name = "Category Management", description = "APIs for managing categories and sub-categories")
public class CategoryController {

    private final CategoryService categoryService;

    @GetMapping
    @Operation(summary = "Get all categories", description = "Retrieve all categories in 2-level hierarchy (parent -> children)")
    public ResponseEntity<ApiResponse<List<CategoryResponse>>> getAllCategories() {
        List<CategoryResponse> categories = categoryService.getAllCategoriesHierarchy();
        return ResponseEntity.ok(ApiResponse.ok(categories, "Danh mục đã được lấy thành công"));
    }

    @GetMapping("/parents")
    @Operation(summary = "Get all parent categories", description = "Retrieve all top-level categories without children")
    public ResponseEntity<ApiResponse<List<CategoryResponse>>> getParentCategories() {
        List<CategoryResponse> categories = categoryService.getAllParentCategories();
        return ResponseEntity.ok(ApiResponse.ok(categories, "Danh mục cha đã được lấy thành công"));
    }

    /* We don't use this endpoint on product, but the endpoint below */
    @GetMapping("/id/{id}")
    @Operation(summary = "Get category by ID", description = "Retrieve a specific category with its children")
    public ResponseEntity<ApiResponse<CategoryResponse>> getCategoryById(
            @Parameter(description = "Category ID", example = "1") @PathVariable Integer id) {
        CategoryResponse category = categoryService.getCategoryById(id);
        return ResponseEntity.ok(ApiResponse.ok(category, "Danh mục đã được lấy thành công"));
    }

    @GetMapping("/slug")
    @Operation(summary = "Get category by slug", description = "Retrieve a specific category by its slug. Supports hierarchical slugs with forward slashes (e.g., 'dien-tu/dien-thoai-di-dong')")
    public ResponseEntity<ApiResponse<CategoryResponse>> getCategoryBySlug(
            @Parameter(description = "Category slug (can include forward slashes for hierarchical categories)", example = "dien-tu/dien-thoai-di-dong") @RequestParam String slug) {
        CategoryResponse category = categoryService.getCategoryBySlug(slug);
        return ResponseEntity.ok(ApiResponse.ok(category, "Danh mục đã được lấy thành công"));
    }

    @GetMapping("/{parentId}/sub-categories")
    @Operation(summary = "Get sub-categories", description = "Retrieve all sub-categories of a parent category")
    public ResponseEntity<ApiResponse<List<CategoryResponse>>> getSubCategories(
            @Parameter(description = "Parent category ID", example = "1") @PathVariable Integer parentId) {
        List<CategoryResponse> subCategories = categoryService.getSubCategories(parentId);
        return ResponseEntity.ok(ApiResponse.ok(subCategories, "Sub-categories đã được lấy thành công"));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Create category (ADMIN only)", description = "Create a new parent category or sub-category (max 2 levels). Only accessible by administrators.")
    public ResponseEntity<ApiResponse<CategoryResponse>> createCategory(
            @Valid @RequestBody CreateCategoryRequest request) {
        CategoryResponse category = categoryService.createCategory(request);
        return ResponseEntity.status(201)
                .body(ApiResponse.created(category, "Danh mục đã được tạo thành công"));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Update category (ADMIN only)", description = "Update category name and/or parent. Only accessible by administrators.")
    public ResponseEntity<ApiResponse<CategoryResponse>> updateCategory(
            @Parameter(description = "Category ID", example = "1") @PathVariable Integer id,
            @Valid @RequestBody CreateCategoryRequest request) {
        CategoryResponse category = categoryService.updateCategory(id, request);
        return ResponseEntity.ok(ApiResponse.ok(category, "Danh mục đã được cập nhật thành công"));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Delete category (ADMIN only)", description = "Delete a category (only if it has no products and no sub-categories). Only accessible by administrators.")
    public ResponseEntity<ApiResponse<Void>> deleteCategory(
            @Parameter(description = "Category ID", example = "1") @PathVariable Integer id) {
        categoryService.deleteCategory(id);
        return ResponseEntity.ok(ApiResponse.ok(null, "Danh mục đã được xóa thành công"));
    }
}
