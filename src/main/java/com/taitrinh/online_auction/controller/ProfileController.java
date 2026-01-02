package com.taitrinh.online_auction.controller;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
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
import com.taitrinh.online_auction.dto.profile.BiddingProductResponse;
import com.taitrinh.online_auction.dto.profile.ChangePasswordRequest;
import com.taitrinh.online_auction.dto.profile.CreateReviewRequest;
import com.taitrinh.online_auction.dto.profile.FavoriteProductResponse;
import com.taitrinh.online_auction.dto.profile.ReviewResponse;
import com.taitrinh.online_auction.dto.profile.RevieweeProfileResponse;
import com.taitrinh.online_auction.dto.profile.SellerActiveProductResponse;
import com.taitrinh.online_auction.dto.profile.SellerEndedProductResponse;
import com.taitrinh.online_auction.dto.profile.UpdateProfileRequest;
import com.taitrinh.online_auction.dto.profile.UpdateReviewRequest;
import com.taitrinh.online_auction.dto.profile.UserProfileResponse;
import com.taitrinh.online_auction.dto.profile.WonProductResponse;
import com.taitrinh.online_auction.security.UserDetailsImpl;
import com.taitrinh.online_auction.service.ProfileService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/profile")
@RequiredArgsConstructor
@Validated
@Tag(name = "Profile Management", description = "User profile management endpoints for bidders")
@SecurityRequirement(name = "Bearer Authentication")
public class ProfileController {

        private final ProfileService profileService;

        @GetMapping
        @Operation(summary = "Get current user profile", description = "Get current authenticated user's profile information with rating statistics")
        @io.swagger.v3.oas.annotations.responses.ApiResponses(value = {
                        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Profile retrieved successfully"),
                        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized")
        })
        public ResponseEntity<ApiResponse<UserProfileResponse>> getProfile(
                        @AuthenticationPrincipal UserDetailsImpl userDetails) {
                UserProfileResponse profile = profileService.getUserProfile(userDetails.getUserId());
                return ResponseEntity.ok(ApiResponse.ok(profile, "Thông tin tài khoản đã được lấy thành công"));
        }

        @PutMapping
        @Operation(summary = "Update user profile", description = "Update user profile information. All fields are optional but typically all are sent together. Email change requires re-verification.")
        @io.swagger.v3.oas.annotations.responses.ApiResponses(value = {
                        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Profile updated successfully"),
                        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid input or email already exists"),
                        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized")
        })
        public ResponseEntity<ApiResponse<UserProfileResponse>> updateProfile(
                        @AuthenticationPrincipal UserDetailsImpl userDetails,
                        @Valid @RequestBody UpdateProfileRequest request) {
                UserProfileResponse profile = profileService.updateProfile(userDetails.getUserId(), request);
                return ResponseEntity.ok(ApiResponse.ok(profile, "Thông tin tài khoản đã được cập nhật thành công"));
        }

        @PutMapping("/password")
        @Operation(summary = "Change password", description = "Change user password. Requires old password for verification.")
        @io.swagger.v3.oas.annotations.responses.ApiResponses(value = {
                        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Password changed successfully"),
                        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid old password or password requirements not met"),
                        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized")
        })
        public ResponseEntity<ApiResponse<Void>> changePassword(
                        @AuthenticationPrincipal UserDetailsImpl userDetails,
                        @Valid @RequestBody ChangePasswordRequest request) {
                profileService.changePassword(userDetails.getUserId(), request);
                return ResponseEntity.ok(ApiResponse.ok(null, "Mật khẩu đã được thay đổi thành công"));
        }

        @GetMapping("/reviews")
        @Operation(summary = "Get user reviews", description = "Get all reviews received by the current user with pagination")
        @io.swagger.v3.oas.annotations.responses.ApiResponses(value = {
                        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Reviews retrieved successfully"),
                        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized")
        })
        public ResponseEntity<ApiResponse<Page<ReviewResponse>>> getUserReviews(
                        @AuthenticationPrincipal UserDetailsImpl userDetails,
                        @Parameter(description = "Page number (0-indexed)", example = "0") @RequestParam(defaultValue = "0") @Min(0) Integer page,
                        @Parameter(description = "Page size", example = "10") @RequestParam(defaultValue = "10") @Min(1) @Max(100) Integer size) {
                Page<ReviewResponse> reviews = profileService.getUserReviews(userDetails.getUserId(),
                                PageRequest.of(page, size));
                return ResponseEntity.ok(ApiResponse.ok(reviews, "Danh sách đánh giá đã được lấy thành công"));
        }

        @GetMapping("/reviews/given")
        @Operation(summary = "Get reviews given by user", description = "Get all reviews made by the current user (where user is the reviewer)")
        @io.swagger.v3.oas.annotations.responses.ApiResponses(value = {
                        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Reviews retrieved successfully"),
                        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized")
        })
        public ResponseEntity<ApiResponse<Page<ReviewResponse>>> getReviewsGivenByUser(
                        @AuthenticationPrincipal UserDetailsImpl userDetails,
                        @Parameter(description = "Page number (0-indexed)", example = "0") @RequestParam(defaultValue = "0") @Min(0) Integer page,
                        @Parameter(description = "Page size", example = "10") @RequestParam(defaultValue = "10") @Min(1) @Max(100) Integer size) {
                Page<ReviewResponse> reviews = profileService.getReviewsGivenByUser(userDetails.getUserId(),
                                PageRequest.of(page, size));
                return ResponseEntity.ok(ApiResponse.ok(reviews, "Danh sách đánh giá đã gửi đã được lấy thành công"));
        }

        @PostMapping("/reviews")
        @Operation(summary = "Create review", description = "Create a review for a seller or buyer after auction ends. Only winners can review sellers and sellers can review winners.")
        @io.swagger.v3.oas.annotations.responses.ApiResponses(value = {
                        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Review created successfully"),
                        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid request or review criteria not met"),
                        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized")
        })
        public ResponseEntity<ApiResponse<Void>> createReview(
                        @AuthenticationPrincipal UserDetailsImpl userDetails,
                        @Valid @RequestBody CreateReviewRequest request) {
                profileService.createReview(userDetails.getUserId(), request);
                return ResponseEntity.ok(ApiResponse.ok(null, "Đánh giá đã được tạo thành công"));
        }

        @PutMapping("/reviews/{productId}")
        @Operation(summary = "Update review", description = "Update an existing review for a product. Users can only update their own reviews.")
        @io.swagger.v3.oas.annotations.responses.ApiResponses(value = {
                        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Review updated successfully"),
                        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid request or review not found"),
                        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized")
        })
        public ResponseEntity<ApiResponse<Void>> updateReview(
                        @AuthenticationPrincipal UserDetailsImpl userDetails,
                        @Parameter(description = "Product ID", example = "1") @PathVariable Long productId,
                        @Valid @RequestBody UpdateReviewRequest request) {
                profileService.updateReview(userDetails.getUserId(), productId, request);
                return ResponseEntity.ok(ApiResponse.ok(null, "Đánh giá đã được cập nhật thành công"));
        }

        @DeleteMapping("/reviews/{productId}")
        @Operation(summary = "Delete review", description = "Delete an existing review for a product. Users can only delete their own reviews.")
        @io.swagger.v3.oas.annotations.responses.ApiResponses(value = {
                        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Review deleted successfully"),
                        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Review not found"),
                        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized")
        })
        public ResponseEntity<ApiResponse<Void>> deleteReview(
                        @AuthenticationPrincipal UserDetailsImpl userDetails,
                        @Parameter(description = "Product ID", example = "1") @PathVariable Long productId) {
                profileService.deleteReview(userDetails.getUserId(), productId);
                return ResponseEntity.ok(ApiResponse.ok(null, "Đánh giá đã được xóa thành công"));
        }

        @GetMapping("/reviewee/{productId}")
        @Operation(summary = "Get reviewee profile", description = "Get basic profile information of the person to review (seller or winner). Only seller and winner can access this for a specific product.")
        @io.swagger.v3.oas.annotations.responses.ApiResponses(value = {
                        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Profile retrieved successfully"),
                        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Product not ended or no winner, or unauthorized access"),
                        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Product not found"),
                        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized")
        })
        public ResponseEntity<ApiResponse<RevieweeProfileResponse>> getRevieweeProfile(
                        @AuthenticationPrincipal UserDetailsImpl userDetails,
                        @Parameter(description = "Product ID", example = "1") @PathVariable Long productId) {
                RevieweeProfileResponse profile = profileService.getRevieweeProfile(productId, userDetails.getUserId());
                return ResponseEntity.ok(ApiResponse.ok(profile, "Lấy thông tin người đánh giá thành công"));
        }

        @GetMapping("/reviews/product/{productId}")
        @Operation(summary = "Get user's review for product", description = "Get the current user's review for a specific product. Returns null if no review exists.")
        @io.swagger.v3.oas.annotations.responses.ApiResponses(value = {
                        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Review retrieved successfully (or null if not found)"),
                        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized")
        })
        public ResponseEntity<ApiResponse<ReviewResponse>> getUserReviewForProduct(
                        @AuthenticationPrincipal UserDetailsImpl userDetails,
                        @Parameter(description = "Product ID", example = "1") @PathVariable Long productId) {
                ReviewResponse review = profileService.getUserReviewForProduct(productId, userDetails.getUserId());
                return ResponseEntity.ok(ApiResponse.ok(review,
                                review != null ? "Đánh giá đã được tìm thấy" : "Bạn chưa đánh giá sản phẩm này"));
        }

        @GetMapping("/favorites")
        @Operation(summary = "Get favorite products", description = "Get all products in user's favorites/watchlist with pagination")
        @io.swagger.v3.oas.annotations.responses.ApiResponses(value = {
                        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Favorites retrieved successfully"),
                        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized")
        })
        public ResponseEntity<ApiResponse<Page<FavoriteProductResponse>>> getFavoriteProducts(
                        @AuthenticationPrincipal UserDetailsImpl userDetails,
                        @Parameter(description = "Page number (0-indexed)", example = "0") @RequestParam(defaultValue = "0") @Min(0) Integer page,
                        @Parameter(description = "Page size", example = "10") @RequestParam(defaultValue = "10") @Min(1) @Max(100) Integer size) {
                Page<FavoriteProductResponse> favorites = profileService.getFavoriteProducts(userDetails.getUserId(),
                                PageRequest.of(page, size));
                return ResponseEntity.ok(ApiResponse.ok(favorites,
                                "Danh sách sản phẩm đã được yêu thích đã được lấy thành công"));
        }

        @PostMapping("/favorites/{productId}")
        @Operation(summary = "Add product to favorites", description = "Add a product to user's favorites/watchlist", security = @SecurityRequirement(name = "Bearer Authentication"))
        @io.swagger.v3.oas.annotations.responses.ApiResponses(value = {
                        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "Product added to favorites"),
                        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Product already in favorites"),
                        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Product not found"),
                        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized")
        })
        public ResponseEntity<ApiResponse<Void>> addFavorite(
                        @AuthenticationPrincipal UserDetailsImpl userDetails,
                        @Parameter(description = "Product ID to add to favorites", example = "1") @PathVariable Long productId) {
                profileService.addFavorite(userDetails.getUserId(), productId);
                return ResponseEntity.status(HttpStatus.CREATED)
                                .body(ApiResponse.ok(null, "Sản phẩm đã được thêm vào danh sách yêu thích"));
        }

        @DeleteMapping("/favorites/{productId}")
        @Operation(summary = "Remove product from favorites", description = "Remove a product from user's favorites/watchlist", security = @SecurityRequirement(name = "Bearer Authentication"))
        @io.swagger.v3.oas.annotations.responses.ApiResponses(value = {
                        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Product removed from favorites"),
                        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Favorite not found"),
                        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized")
        })
        public ResponseEntity<ApiResponse<Void>> removeFavorite(
                        @AuthenticationPrincipal UserDetailsImpl userDetails,
                        @Parameter(description = "Product ID to remove from favorites", example = "1") @PathVariable Long productId) {
                profileService.removeFavorite(userDetails.getUserId(), productId);
                return ResponseEntity.ok(ApiResponse.ok(null, "Sản phẩm đã được xóa khỏi danh sách yêu thích"));
        }

        @GetMapping("/favorites/check/{productId}")
        @Operation(summary = "Check if product is favorited", description = "Check whether a specific product is in user's favorites/watchlist", security = @SecurityRequirement(name = "Bearer Authentication"))
        @io.swagger.v3.oas.annotations.responses.ApiResponses(value = {
                        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Check completed successfully"),
                        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized")
        })
        public ResponseEntity<ApiResponse<Boolean>> checkFavorite(
                        @AuthenticationPrincipal UserDetailsImpl userDetails,
                        @Parameter(description = "Product ID to check", example = "1") @PathVariable Long productId) {
                boolean isFavorited = profileService.isFavorited(userDetails.getUserId(), productId);
                return ResponseEntity.ok(ApiResponse.ok(isFavorited,
                                isFavorited ? "Sản phẩm đã có trong danh sách yêu thích"
                                                : "Sản phẩm chưa có trong danh sách yêu thích"));
        }

        @GetMapping("/bidding")
        @Operation(summary = "Get products currently bidding on", description = "Get all active products where user has placed bids")
        @io.swagger.v3.oas.annotations.responses.ApiResponses(value = {
                        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Bidding products retrieved successfully"),
                        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized")
        })
        public ResponseEntity<ApiResponse<Page<BiddingProductResponse>>> getBiddingProducts(
                        @AuthenticationPrincipal UserDetailsImpl userDetails,
                        @Parameter(description = "Page number (0-indexed)", example = "0") @RequestParam(defaultValue = "0") @Min(0) Integer page,
                        @Parameter(description = "Page size", example = "10") @RequestParam(defaultValue = "10") @Min(1) @Max(100) Integer size) {
                Page<BiddingProductResponse> biddingProducts = profileService.getBiddingProducts(
                                userDetails.getUserId(),
                                PageRequest.of(page, size));
                return ResponseEntity.ok(ApiResponse.ok(biddingProducts,
                                "Danh sách sản phẩm đang đấu giá đã được lấy thành công"));
        }

        @GetMapping("/won")
        @Operation(summary = "Get won products", description = "Get all products won by user (where user was highest bidder and auction ended)")
        @io.swagger.v3.oas.annotations.responses.ApiResponses(value = {
                        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Won products retrieved successfully"),
                        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized")
        })
        public ResponseEntity<ApiResponse<Page<WonProductResponse>>> getWonProducts(
                        @AuthenticationPrincipal UserDetailsImpl userDetails,
                        @Parameter(description = "Page number (0-indexed)", example = "0") @RequestParam(defaultValue = "0") @Min(0) Integer page,
                        @Parameter(description = "Page size", example = "10") @RequestParam(defaultValue = "10") @Min(1) @Max(100) Integer size) {
                Page<WonProductResponse> wonProducts = profileService.getWonProducts(userDetails.getUserId(),
                                PageRequest.of(page, size));
                return ResponseEntity
                                .ok(ApiResponse.ok(wonProducts, "Danh sách sản phẩm đã thắng đã được lấy thành công"));
        }

        @GetMapping("/seller/products/active")
        @Operation(summary = "Get seller's active products", description = "Get all active (not ended) products for the current seller")
        @io.swagger.v3.oas.annotations.responses.ApiResponses(value = {
                        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Products retrieved successfully"),
                        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized"),
                        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Not a seller")
        })
        public ResponseEntity<ApiResponse<Page<SellerActiveProductResponse>>> getActiveSellerProducts(
                        @AuthenticationPrincipal UserDetailsImpl userDetails,
                        @Parameter(description = "Page number (0-indexed)", example = "0") @RequestParam(defaultValue = "0") @Min(0) Integer page,
                        @Parameter(description = "Page size", example = "10") @RequestParam(defaultValue = "10") @Min(1) @Max(100) Integer size) {
                Page<SellerActiveProductResponse> products = profileService.getActiveSellerProducts(
                                userDetails.getUserId(),
                                PageRequest.of(page, size));
                return ResponseEntity.ok(ApiResponse.ok(products, "Danh sách sản phẩm đang bán"));
        }

        @GetMapping("/seller/products/ended")
        @Operation(summary = "Get seller's ended products with winners", description = "Get all ended products with winners for the current seller. These products can be reviewed.")
        @io.swagger.v3.oas.annotations.responses.ApiResponses(value = {
                        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Products retrieved successfully"),
                        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized"),
                        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Not a seller")
        })
        public ResponseEntity<ApiResponse<Page<SellerEndedProductResponse>>> getEndedProductsWithWinners(
                        @AuthenticationPrincipal UserDetailsImpl userDetails,
                        @Parameter(description = "Page number (0-indexed)", example = "0") @RequestParam(defaultValue = "0") @Min(0) Integer page,
                        @Parameter(description = "Page size", example = "10") @RequestParam(defaultValue = "10") @Min(1) @Max(100) Integer size) {
                Page<SellerEndedProductResponse> products = profileService.getEndedProductsWithWinners(
                                userDetails.getUserId(),
                                PageRequest.of(page, size));
                return ResponseEntity.ok(ApiResponse.ok(products, "Danh sách sản phẩm đã kết thúc có người thắng"));
        }
}
