package com.taitrinh.online_auction.controller;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.taitrinh.online_auction.dto.ApiResponse;
import com.taitrinh.online_auction.dto.product.BidHistoryResponse;
import com.taitrinh.online_auction.dto.product.CreateProductRequest;
import com.taitrinh.online_auction.dto.product.CreateProductResponse;
import com.taitrinh.online_auction.dto.product.ProductDetailResponse;
import com.taitrinh.online_auction.dto.product.ProductListResponse;
import com.taitrinh.online_auction.dto.product.ProductSearchRequest;
import com.taitrinh.online_auction.dto.product.UpdateProductDescriptionRequest;
import com.taitrinh.online_auction.security.UserDetailsImpl;
import com.taitrinh.online_auction.service.ProductService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/products")
@RequiredArgsConstructor
@Tag(name = "Product Management", description = "APIs for browsing, searching, and viewing products")
public class ProductController {

        private final ProductService productService;

        @GetMapping("/top/ending-soon")
        @Operation(summary = "Get top 5 products ending soon", description = "Retrieve 5 products with nearest end time (for homepage)")
        public ResponseEntity<ApiResponse<List<ProductListResponse>>> getTop5EndingSoon() {
                List<ProductListResponse> products = productService.getTop5EndingSoon();
                return ResponseEntity.ok(ApiResponse.ok(products,
                                "Top 5 products ending soon retrieved successfully"));
        }

        @GetMapping("/top/most-bids")
        @Operation(summary = "Get top 5 products with most bids", description = "Retrieve 5 products with highest bid count (for homepage)")
        public ResponseEntity<ApiResponse<List<ProductListResponse>>> getTop5ByBidCount() {
                List<ProductListResponse> products = productService.getTop5ByBidCount();
                return ResponseEntity.ok(ApiResponse.ok(products,
                                "Top 5 products by bid count retrieved successfully"));
        }

        @GetMapping("/top/highest-price")
        @Operation(summary = "Get top 5 products with highest price", description = "Retrieve 5 products with highest current price (for homepage)")
        public ResponseEntity<ApiResponse<List<ProductListResponse>>> getTop5ByPrice() {
                List<ProductListResponse> products = productService.getTop5ByPrice();
                return ResponseEntity.ok(ApiResponse.ok(products,
                                "Top 5 products by price retrieved successfully"));
        }

        @GetMapping("/category/{categoryId}")
        @Operation(summary = "Get products by category", description = "Retrieve all products in a specific category (including sub-categories) with pagination")
        public ResponseEntity<ApiResponse<Page<ProductListResponse>>> getProductsByCategory(
                        @Parameter(description = "Category ID", example = "1") @PathVariable Integer categoryId,

                        @Parameter(description = "Page number (0-indexed)", example = "0") @RequestParam(defaultValue = "0") @Min(0) Integer page,

                        @Parameter(description = "Page size", example = "20") @RequestParam(defaultValue = "20") @Min(1) Integer size,

                        @Parameter(description = "Sort field", example = "endTime") @RequestParam(defaultValue = "endTime") String sortBy,

                        @Parameter(description = "Sort direction (asc/desc)", example = "asc") @RequestParam(defaultValue = "asc") String sortDirection) {

                Page<ProductListResponse> products = productService.getProductsByCategory(
                                categoryId, page, size, sortBy, sortDirection);
                return ResponseEntity.ok(ApiResponse.ok(products,
                                "Products by category retrieved successfully"));
        }

        @GetMapping("/search")
        @Operation(summary = "Search products", description = "Search products by keyword and/or category with pagination and sorting. "
                        +
                        "Supports Vietnamese full-text search without diacritics.")
        public ResponseEntity<ApiResponse<Page<ProductListResponse>>> searchProducts(
                        @Parameter(description = "Search keyword", example = "iPhone") @RequestParam(required = false) String keyword,

                        @Parameter(description = "Category ID to filter", example = "5") @RequestParam(required = false) Integer categoryId,

                        @Parameter(description = "Page number (0-indexed)", example = "0") @RequestParam(defaultValue = "0") @Min(0) Integer page,

                        @Parameter(description = "Page size", example = "20") @RequestParam(defaultValue = "20") @Min(1) Integer size,

                        @Parameter(description = "Sort field (endTime, currentPrice, createdAt, bidCount)", example = "endTime") @RequestParam(defaultValue = "endTime") String sortBy,

                        @Parameter(description = "Sort direction (asc/desc)", example = "asc") @RequestParam(defaultValue = "asc") String sortDirection) {

                ProductSearchRequest request = ProductSearchRequest.builder()
                                .keyword(keyword)
                                .categoryId(categoryId)
                                .page(page)
                                .size(size)
                                .sortBy(sortBy)
                                .sortDirection(sortDirection)
                                .build();

                Page<ProductListResponse> products = productService.searchProducts(request);
                return ResponseEntity.ok(ApiResponse.ok(products,
                                "Products search completed successfully"));
        }

        @GetMapping("/id/{id}")
        @Operation(summary = "Get product details by ID", description = "Retrieve complete information about a specific product including images, "
                        +
                        "seller info, highest bidder info, and description logs")
        public ResponseEntity<ApiResponse<ProductDetailResponse>> getProductDetail(
                        @Parameter(description = "Product ID", example = "1") @PathVariable Long id) {

                ProductDetailResponse product = productService.getProductDetail(id);
                return ResponseEntity.ok(ApiResponse.ok(product,
                                "Product details retrieved successfully"));
        }

        @GetMapping("/slug/{slug}")
        @Operation(summary = "Get product details by slug", description = "Retrieve complete information about a specific product including images, "
                        +
                        "seller info, highest bidder info, and description logs")
        public ResponseEntity<ApiResponse<ProductDetailResponse>> getProductDetailBySlug(
                        @Parameter(description = "Product slug", example = "iphone-15-pro-max") @PathVariable String slug) {

                ProductDetailResponse product = productService.getProductDetailBySlug(slug);
                return ResponseEntity.ok(ApiResponse.ok(product,
                                "Product details retrieved successfully"));
        }

        @GetMapping("/{id}/related")
        @Operation(summary = "Get related products", description = "Get 5 other products in the same category (excluding current product)")
        public ResponseEntity<ApiResponse<List<ProductListResponse>>> getRelatedProducts(
                        @Parameter(description = "Product ID", example = "1") @PathVariable Long id) {

                List<ProductListResponse> products = productService.getRelatedProducts(id);
                return ResponseEntity.ok(ApiResponse.ok(products,
                                "Related products retrieved successfully"));
        }

        @GetMapping("/{id}/bid-history")
        @Operation(summary = "Get bid history", description = "Retrieve bid history for a product with masked bidder information. "
                        +
                        "Bidder names are partially hidden (e.g., ****Khoa)")
        public ResponseEntity<ApiResponse<List<BidHistoryResponse>>> getBidHistory(
                        @Parameter(description = "Product ID", example = "1") @PathVariable Long id) {

                List<BidHistoryResponse> bidHistory = productService.getBidHistory(id);
                return ResponseEntity.ok(ApiResponse.ok(bidHistory,
                                "Bid history retrieved successfully"));
        }

        @PostMapping
        @Operation(summary = "Create auction product", description = "Create a new auction product (Seller only). Requires at least 3 images, one must be marked as primary. Seller ID is automatically extracted from authentication token.", security = @SecurityRequirement(name = "Bearer Authentication"))
        public ResponseEntity<ApiResponse<CreateProductResponse>> createProduct(
                        @Valid @RequestBody CreateProductRequest request,
                        @AuthenticationPrincipal UserDetailsImpl userDetails) {

                CreateProductResponse product = productService.createProduct(request, userDetails.getUserId());
                return ResponseEntity.status(HttpStatus.CREATED)
                                .body(ApiResponse.ok(product, "Product created successfully"));
        }

        @PutMapping("/{id}/description")
        @Operation(summary = "Update product description", description = "Append additional description to existing product (Seller only). Cannot replace existing description, only append. Seller ID is automatically extracted from authentication token.", security = @SecurityRequirement(name = "Bearer Authentication"))
        public ResponseEntity<ApiResponse<Void>> updateProductDescription(
                        @Parameter(description = "Product ID", example = "1") @PathVariable Long id,

                        @Valid @RequestBody UpdateProductDescriptionRequest request,

                        @AuthenticationPrincipal UserDetailsImpl userDetails) {

                productService.updateProductDescription(id, userDetails.getUserId(), request);
                return ResponseEntity.ok(ApiResponse.ok(null, "Product description updated successfully"));
        }
}