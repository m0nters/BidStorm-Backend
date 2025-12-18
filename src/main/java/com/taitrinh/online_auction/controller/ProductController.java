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
                                "Top 5 sản phẩm sắp kết thúc đã được lấy thành công"));
        }

        @GetMapping("/top/most-bids")
        @Operation(summary = "Get top 5 products with most bids", description = "Retrieve 5 products with highest bid count (for homepage)")
        public ResponseEntity<ApiResponse<List<ProductListResponse>>> getTop5ByBidCount() {
                List<ProductListResponse> products = productService.getTop5ByBidCount();
                return ResponseEntity.ok(ApiResponse.ok(products,
                                "Top 5 sản phẩm với số lượng đấu giá cao nhất đã được lấy thành công"));
        }

        @GetMapping("/top/highest-price")
        @Operation(summary = "Get top 5 products with highest price", description = "Retrieve 5 products with highest current price (for homepage)")
        public ResponseEntity<ApiResponse<List<ProductListResponse>>> getTop5ByPrice() {
                List<ProductListResponse> products = productService.getTop5ByPrice();
                return ResponseEntity.ok(ApiResponse.ok(products,
                                "Top 5 sản phẩm với giá cao nhất đã được lấy thành công"));
        }

        @GetMapping("/category/{categoryId}")
        @Operation(summary = "Get products by category", description = "Retrieve products by category with automatic detection. "
                        + "If the category is a parent category (has subcategories), returns products from all subcategories. "
                        + "If the category is a leaf category, returns products from that specific category only. "
                        + "Supports pagination and sorting.")
        public ResponseEntity<ApiResponse<Page<ProductListResponse>>> getProductsByCategory(
                        @Parameter(description = "Category ID (works with both parent and leaf categories)", example = "1") @PathVariable Integer categoryId,

                        @Parameter(description = "Page number (0-indexed)", example = "0") @RequestParam(defaultValue = "0") @Min(0) Integer page,

                        @Parameter(description = "Page size", example = "20") @RequestParam(defaultValue = "20") @Min(1) Integer size,

                        @Parameter(description = "Sort field", example = "endTime") @RequestParam(defaultValue = "endTime") String sortBy,

                        @Parameter(description = "Sort direction (asc/desc)", example = "asc") @RequestParam(defaultValue = "asc") String sortDirection) {

                Page<ProductListResponse> products = productService.getProductsByCategory(
                                categoryId, page, size, sortBy, sortDirection);
                return ResponseEntity.ok(ApiResponse.ok(products,
                                "Sản phẩm trong danh mục đã được lấy thành công"));
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
                                "Tìm kiếm sản phẩm đã hoàn thành thành công"));
        }

        /* We don't use this endpoint on product, but the endpoint below */
        @GetMapping("/id/{id}")
        @Operation(summary = "Get product details by ID", description = "(For developer only) Retrieve complete information about a specific product including images, "
                        +
                        "seller info, highest bidder info, and description logs")
        public ResponseEntity<ApiResponse<ProductDetailResponse>> getProductDetail(
                        @Parameter(description = "Product ID", example = "1") @PathVariable Long id) {

                ProductDetailResponse product = productService.getProductDetailById(id);
                return ResponseEntity.ok(ApiResponse.ok(product,
                                "Chi tiết sản phẩm đã được lấy thành công"));
        }

        @GetMapping("/slug/{slug}")
        @Operation(summary = "Get product details by slug", description = "Retrieve complete information about a specific product including images, "
                        +
                        "seller info, highest bidder info, and description logs")
        public ResponseEntity<ApiResponse<ProductDetailResponse>> getProductDetailBySlug(
                        @Parameter(description = "Product slug", example = "iphone-15-pro-max") @PathVariable String slug) {

                ProductDetailResponse product = productService.getProductDetailBySlug(slug);
                return ResponseEntity.ok(ApiResponse.ok(product,
                                "Chi tiết sản phẩm đã được lấy thành công"));
        }

        @GetMapping("/{id}/related")
        @Operation(summary = "Get related products", description = "Get 5 other products in the same category (excluding current product)")
        public ResponseEntity<ApiResponse<List<ProductListResponse>>> getRelatedProducts(
                        @Parameter(description = "Product ID", example = "1") @PathVariable Long id) {

                List<ProductListResponse> products = productService.getRelatedProducts(id);
                return ResponseEntity.ok(ApiResponse.ok(products,
                                "Sản phẩm liên quan đã được lấy thành công"));
        }

        @GetMapping("/{id}/bid-history")
        @Operation(summary = "Get bid history", description = "Retrieve bid history for a product with masked bidder information. "
                        +
                        "Bidder names are partially hidden (e.g., ****Khoa)")
        public ResponseEntity<ApiResponse<List<BidHistoryResponse>>> getBidHistory(
                        @Parameter(description = "Product ID", example = "1") @PathVariable Long id) {

                List<BidHistoryResponse> bidHistory = productService.getBidHistory(id);
                return ResponseEntity.ok(ApiResponse.ok(bidHistory,
                                "Lịch sử đấu giá đã được lấy thành công"));
        }

        @PostMapping
        @Operation(summary = "Create auction product", description = "Create a new auction product (Seller only). Requires at least 3 images, one must be marked as primary. Seller ID is automatically extracted from authentication token.", security = @SecurityRequirement(name = "Bearer Authentication"))
        public ResponseEntity<ApiResponse<CreateProductResponse>> createProduct(
                        @Valid @RequestBody CreateProductRequest request,
                        @AuthenticationPrincipal UserDetailsImpl userDetails) {

                CreateProductResponse product = productService.createProduct(request, userDetails.getUserId());
                return ResponseEntity.status(HttpStatus.CREATED)
                                .body(ApiResponse.ok(product, "Sản phẩm đã được tạo thành công"));
        }

        @PutMapping("/{id}/description")
        @Operation(summary = "Update product description", description = "Append additional description to existing product (Seller only). Cannot replace existing description, only append. Seller ID is automatically extracted from authentication token.", security = @SecurityRequirement(name = "Bearer Authentication"))
        public ResponseEntity<ApiResponse<Void>> updateProductDescription(
                        @Parameter(description = "Product ID", example = "1") @PathVariable Long id,

                        @Valid @RequestBody UpdateProductDescriptionRequest request,

                        @AuthenticationPrincipal UserDetailsImpl userDetails) {

                productService.updateProductDescription(id, userDetails.getUserId(), request);
                return ResponseEntity.ok(ApiResponse.ok(null, "Mô tả sản phẩm đã được cập nhật thành công"));
        }
}