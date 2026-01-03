package com.taitrinh.online_auction.service;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

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
import com.taitrinh.online_auction.entity.BidHistory;
import com.taitrinh.online_auction.entity.Favorite;
import com.taitrinh.online_auction.entity.OrderCompletion;
import com.taitrinh.online_auction.entity.Product;
import com.taitrinh.online_auction.entity.ProductImage;
import com.taitrinh.online_auction.entity.Review;
import com.taitrinh.online_auction.entity.User;
import com.taitrinh.online_auction.exception.BadRequestException;
import com.taitrinh.online_auction.exception.EmailAlreadyExistsException;
import com.taitrinh.online_auction.exception.ResourceNotFoundException;
import com.taitrinh.online_auction.repository.BidHistoryRepository;
import com.taitrinh.online_auction.repository.FavoriteRepository;
import com.taitrinh.online_auction.repository.OrderCompletionRepository;
import com.taitrinh.online_auction.repository.ProductRepository;
import com.taitrinh.online_auction.repository.ReviewRepository;
import com.taitrinh.online_auction.repository.UserRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProfileService {

    private final UserRepository userRepository;
    private final ReviewRepository reviewRepository;
    private final FavoriteRepository favoriteRepository;
    private final ProductRepository productRepository;
    private final BidHistoryRepository bidHistoryRepository;
    private final OrderCompletionRepository orderCompletionRepository;
    private final PasswordEncoder passwordEncoder;
    private final S3Service s3Service;

    /**
     * Get user profile with rating information
     */
    @Transactional(readOnly = true)
    public UserProfileResponse getUserProfile(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", userId));

        return UserProfileResponse.builder()
                .id(user.getId())
                .email(user.getEmail())
                .fullName(user.getFullName())
                .address(user.getAddress())
                .birthDate(user.getBirthDate())
                .role(user.getRole().getName())
                .avatarUrl(user.getAvatarUrl())
                .positiveRating(user.getPositiveRating())
                .negativeRating(user.getNegativeRating())
                .ratingPercentage(user.getRatingPercentage())
                .totalRatings(user.getPositiveRating() + user.getNegativeRating())
                .emailVerified(user.getEmailVerified())
                .isActive(user.getIsActive())
                .createdAt(user.getCreatedAt())
                .build();
    }

    /**
     * Update user profile information (partial update)
     * Only updates fields that are provided (non-null) in the request
     * If email is changed, emailVerified will be set to false
     */
    @Transactional
    public UserProfileResponse updateProfile(Long userId, UpdateProfileRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", userId));

        boolean updated = false;

        // Update email if provided
        if (request.getEmail() != null && !request.getEmail().isBlank()
                && !java.util.Objects.equals(user.getEmail(), request.getEmail())) {
            // Check if new email already exists
            if (userRepository.existsByEmail(request.getEmail())) {
                throw new EmailAlreadyExistsException(request.getEmail());
            }
            user.setEmail(request.getEmail());
            user.setEmailVerified(false); // Require re-verification for new email
            updated = true;
        }

        // Update full name if provided
        if (request.getFullName() != null && !request.getFullName().isBlank()
                && !java.util.Objects.equals(user.getFullName(), request.getFullName())) {
            user.setFullName(request.getFullName());
            updated = true;
        }

        // Update address if provided
        if (request.getAddress() != null && !request.getAddress().isBlank()
                && !java.util.Objects.equals(user.getAddress(), request.getAddress())) {
            user.setAddress(request.getAddress());
            updated = true;
        }

        // Update birth date if provided
        if (request.getBirthDate() != null
                && !java.util.Objects.equals(user.getBirthDate(), request.getBirthDate())) {
            user.setBirthDate(request.getBirthDate());
            updated = true;
        }

        if (updated) {
            userRepository.save(user);
            log.info("Profile updated for user: {}", userId);
        } else {
            log.debug("No profile fields updated for user: {}", userId);
        }

        return getUserProfile(userId);
    }

    /**
     * Change user password
     * Requires old password verification
     */
    @Transactional
    public void changePassword(Long userId, ChangePasswordRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", userId));

        // Verify old password
        if (!passwordEncoder.matches(request.getOldPassword(), user.getPasswordHash())) {
            throw new BadRequestException("Mật khẩu cũ không chính xác");
        }

        // Encode and set new password
        user.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);

        log.info("Password changed for user: {}", userId);
    }

    /**
     * Get reviews for a user (as reviewee) with pagination
     */
    @Transactional(readOnly = true)
    public Page<ReviewResponse> getUserReviews(Long userId, Pageable pageable) {
        Page<Review> reviews = reviewRepository.findByReviewee_IdOrderByCreatedAtDesc(userId, pageable);

        return reviews.map(review -> {
            String thumbnailUrl = review.getProduct().getImages().stream()
                    .filter(image -> image.getIsPrimary())
                    .findFirst()
                    .map(ProductImage::getUrl)
                    .orElse(null);

            return ReviewResponse.builder()
                    .id(review.getId())
                    .productId(review.getProduct().getId())
                    .productTitle(review.getProduct().getTitle())
                    .productUrl(review.getProduct().getSlug())
                    .thumbnailUrl(thumbnailUrl)
                    .isYourProduct(review.getProduct().getSeller().getId() == userId)
                    .reviewerId(review.getReviewer().getId())
                    .reviewerName(review.getReviewer().getFullName())
                    .rating(review.getRating())
                    .comment(review.getComment())
                    .createdAt(review.getCreatedAt())
                    .build();
        });
    }

    /**
     * Get reviews given by a user (where user is the reviewer) with pagination
     */
    @Transactional(readOnly = true)
    public Page<ReviewResponse> getReviewsGivenByUser(Long userId, Pageable pageable) {
        Page<Review> reviews = reviewRepository.findByReviewer_IdOrderByCreatedAtDesc(userId, pageable);

        return reviews.map(review -> {
            String thumbnailUrl = review.getProduct().getImages().stream()
                    .filter(image -> image.getIsPrimary())
                    .findFirst()
                    .map(ProductImage::getUrl)
                    .orElse(null);

            return ReviewResponse.builder()
                    .id(review.getId())
                    .productId(review.getProduct().getId())
                    .productTitle(review.getProduct().getTitle())
                    .productUrl(review.getProduct().getSlug())
                    .thumbnailUrl(thumbnailUrl)
                    .isYourProduct(review.getProduct().getSeller().getId() == userId)
                    .reviewerId(review.getReviewee().getId()) // Person we reviewed (reviewee)
                    .reviewerName(review.getReviewee().getFullName())
                    .rating(review.getRating())
                    .comment(review.getComment())
                    .createdAt(review.getCreatedAt())
                    .build();
        });
    }

    /**
     * Create a review for a won product
     * Users can only review products they won or sold
     */
    @Transactional
    public void createReview(Long reviewerId, CreateReviewRequest request) {
        // Validate rating value
        if (request.getRating() != 1 && request.getRating() != -1) {
            throw new BadRequestException("Đánh giá phải là 1 hoặc -1");
        }

        // Get the product
        Product product = productRepository.findById(request.getProductId())
                .orElseThrow(() -> new ResourceNotFoundException("Sản phẩm", request.getProductId()));

        // Check if product has ended
        if (!product.getIsEnded()) {
            throw new BadRequestException("Không thể đánh giá sản phẩm chưa kết thúc");
        }

        User reviewer = userRepository.findById(reviewerId)
                .orElseThrow(() -> new ResourceNotFoundException("User", reviewerId));

        // Determine who should be reviewed
        User reviewee;
        if (product.getSeller().getId().equals(reviewerId)) {
            // Seller is reviewing the winner (buyer)
            if (product.getWinner() == null) {
                throw new BadRequestException("Sản phẩm này không có người thắng để đánh giá");
            }
            reviewee = product.getWinner();
        } else if (product.getWinner() != null && product.getWinner().getId().equals(reviewerId)) {
            // Winner is reviewing the seller
            reviewee = product.getSeller();
        } else {
            throw new BadRequestException("Bạn chỉ có thể đánh giá sản phẩm bạn đã thắng hoặc bán");
        }

        // Check if review already exists
        if (reviewRepository.existsByProduct_IdAndReviewer_Id(product.getId(), reviewerId)) {
            throw new BadRequestException("Bạn đã đánh giá giao dịch này rồi");
        }

        // Create review
        Review review = Review.builder()
                .product(product)
                .reviewer(reviewer)
                .reviewee(reviewee)
                .rating(request.getRating())
                .comment(request.getComment())
                .build();

        reviewRepository.save(review);

        // Update reviewee's rating
        if (request.getRating() == 1) {
            reviewee.setPositiveRating(reviewee.getPositiveRating() + 1);
        } else {
            reviewee.setNegativeRating(reviewee.getNegativeRating() + 1);
        }
        userRepository.save(reviewee);

        log.info("Review created by user {} for product {}", reviewerId, request.getProductId());
    }

    /**
     * Update an existing review
     * Users can only update their own reviews
     */
    @Transactional
    public void updateReview(Long reviewerId, Long productId, UpdateReviewRequest request) {
        // Validate rating value
        if (request.getRating() != 1 && request.getRating() != -1) {
            throw new BadRequestException("Đánh giá phải là 1 hoặc -1");
        }

        // Get the review
        Review review = reviewRepository.findByProduct_IdAndReviewer_Id(productId, reviewerId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy đánh giá của bạn cho sản phẩm này"));

        // Verify ownership
        if (!review.getReviewer().getId().equals(reviewerId)) {
            throw new BadRequestException("Bạn chỉ có thể cập nhật đánh giá của chính mình");
        }

        User reviewee = review.getReviewee();
        short oldRating = review.getRating();

        // Update review fields
        review.setRating(request.getRating());
        review.setComment(request.getComment());
        reviewRepository.save(review);

        // Adjust rating counts if rating changed
        if (oldRating != request.getRating()) {
            if (oldRating == 1) {
                reviewee.setPositiveRating(reviewee.getPositiveRating() - 1);
            } else {
                reviewee.setNegativeRating(reviewee.getNegativeRating() - 1);
            }

            if (request.getRating() == 1) {
                reviewee.setPositiveRating(reviewee.getPositiveRating() + 1);
            } else {
                reviewee.setNegativeRating(reviewee.getNegativeRating() + 1);
            }

            userRepository.save(reviewee);
        }

        log.info("Review updated by user {} for product {}", reviewerId, productId);
    }

    /**
     * Delete a review
     * Users can only delete their own reviews
     */
    @Transactional
    public void deleteReview(Long reviewerId, Long productId) {
        // Get the review
        Review review = reviewRepository.findByProduct_IdAndReviewer_Id(productId, reviewerId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy đánh giá của bạn cho sản phẩm này"));

        // Verify ownership
        if (!review.getReviewer().getId().equals(reviewerId)) {
            throw new BadRequestException("Bạn chỉ có thể xóa đánh giá của chính mình");
        }

        User reviewee = review.getReviewee();

        // Decrease rating count
        if (review.getRating() == 1) {
            reviewee.setPositiveRating(reviewee.getPositiveRating() - 1);
        } else {
            reviewee.setNegativeRating(reviewee.getNegativeRating() - 1);
        }
        userRepository.save(reviewee);

        // Delete review
        reviewRepository.delete(review);

        log.info("Review deleted by user {} for product {}", reviewerId, productId);
    }

    /**
     * Internal method to create or update a review programmatically
     * Used by OrderCompletionService when seller cancels order
     * Bypasses authorization checks as this is called internally
     */
    @Transactional
    public void createOrUpdateReviewInternal(Long productId, Long reviewerId, short rating, String comment) {
        // Get the product
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Sản phẩm", productId));

        User reviewer = userRepository.findById(reviewerId)
                .orElseThrow(() -> new ResourceNotFoundException("User", reviewerId));

        // Determine reviewee (for seller canceling, it's the winner)
        User reviewee = product.getWinner();
        if (reviewee == null) {
            throw new BadRequestException("Sản phẩm này không có người thắng");
        }

        // Check if review already exists
        Review existingReview = reviewRepository.findByProduct_IdAndReviewer_Id(productId, reviewerId)
                .orElse(null);

        if (existingReview == null) {
            // Create new review
            Review review = Review.builder()
                    .product(product)
                    .reviewer(reviewer)
                    .reviewee(reviewee)
                    .rating(rating)
                    .comment(comment)
                    .build();

            reviewRepository.save(review);

            // Update reviewee's rating count
            if (rating == 1) {
                reviewee.setPositiveRating(reviewee.getPositiveRating() + 1);
            } else {
                reviewee.setNegativeRating(reviewee.getNegativeRating() + 1);
            }
            userRepository.save(reviewee);

            log.info("Review created internally for product {} by user {}", productId, reviewerId);
        } else {
            // Update existing review
            short oldRating = existingReview.getRating();

            existingReview.setRating(rating);
            existingReview.setComment(comment);
            reviewRepository.save(existingReview);

            // Adjust rating counts if needed
            if (oldRating != rating) {
                if (oldRating == 1) {
                    reviewee.setPositiveRating(reviewee.getPositiveRating() - 1);
                } else {
                    reviewee.setNegativeRating(reviewee.getNegativeRating() - 1);
                }

                if (rating == 1) {
                    reviewee.setPositiveRating(reviewee.getPositiveRating() + 1);
                } else {
                    reviewee.setNegativeRating(reviewee.getNegativeRating() + 1);
                }

                userRepository.save(reviewee);
            }

            log.info("Review updated internally for product {} by user {}", productId, reviewerId);
        }
    }

    /**
     * Get basic profile information of reviewee (person to be reviewed)
     * Only seller and winner can access this for a specific product
     */
    @Transactional(readOnly = true)
    public RevieweeProfileResponse getRevieweeProfile(Long productId, Long requesterId) {
        // Get the product
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Sản phẩm", productId));

        // Check if product has ended
        if (!product.getIsEnded()) {
            throw new BadRequestException("Không thể xem thông tin người đánh giá khi sản phẩm chưa kết thúc");
        }

        // Check if product has winner
        if (product.getWinner() == null) {
            throw new BadRequestException("Sản phẩm này không có người thắng");
        }

        // Determine who is the reviewee based on requester
        User reviewee;
        boolean isSeller = product.getSeller().getId().equals(requesterId);
        boolean isWinner = product.getWinner().getId().equals(requesterId);

        if (isSeller) {
            // Seller wants to see winner's profile
            reviewee = product.getWinner();
        } else if (isWinner) {
            // Winner wants to see seller's profile
            reviewee = product.getSeller();
        } else {
            throw new BadRequestException("Chỉ người bán hoặc người thắng cuộc mới có thể xem thông tin này");
        }

        // Build and return response with basic profile info
        return RevieweeProfileResponse.builder()
                .id(reviewee.getId())
                .fullName(reviewee.getFullName())
                .positiveRating(reviewee.getPositiveRating())
                .negativeRating(reviewee.getNegativeRating())
                .ratingPercentage(reviewee.getRatingPercentage())
                .totalRatings(reviewee.getPositiveRating() + reviewee.getNegativeRating())
                .build();
    }

    /**
     * Get user's review for a specific product
     * Returns the review if it exists, null otherwise
     */
    @Transactional(readOnly = true)
    public ReviewResponse getUserReviewForProduct(Long productId, Long userId) {
        return reviewRepository.findByProduct_IdAndReviewer_Id(productId, userId)
                .map(review -> {
                    String thumbnailUrl = review.getProduct().getImages().isEmpty() ? null
                            : review.getProduct().getImages().get(0).getUrl();

                    return ReviewResponse.builder()
                            .id(review.getId())
                            .productId(review.getProduct().getId())
                            .productTitle(review.getProduct().getTitle())
                            .productUrl(review.getProduct().getSlug())
                            .thumbnailUrl(thumbnailUrl)
                            .reviewerId(review.getReviewee().getId())
                            .reviewerName(review.getReviewee().getFullName())
                            .rating(review.getRating())
                            .comment(review.getComment())
                            .createdAt(review.getCreatedAt())
                            .build();
                })
                .orElse(null);
    }

    /**
     * Get user's favorite products with pagination
     */
    @Transactional(readOnly = true)
    public Page<FavoriteProductResponse> getFavoriteProducts(Long userId, Pageable pageable) {
        Page<Favorite> favorites = favoriteRepository.findByUser_IdOrderByCreatedAtDesc(userId, pageable);

        return favorites.map(favorite -> {
            Product product = favorite.getProduct();
            String thumbnailUrl = product.getImages().isEmpty() ? null
                    : product.getImages().get(0).getUrl();

            return FavoriteProductResponse.builder()
                    .productId(product.getId())
                    .title(product.getTitle())
                    .slug(product.getSlug())
                    .thumbnailUrl(thumbnailUrl)
                    .currentPrice(product.getCurrentPrice())
                    .buyNowPrice(product.getBuyNowPrice())
                    .bidCount(product.getBidCount())
                    .endTime(product.getEndTime())
                    .isEnded(product.getIsEnded())
                    .favoritedAt(favorite.getCreatedAt())
                    .build();
        });
    }

    /**
     * Add a product to user's favorites
     */
    @Transactional
    public void addFavorite(Long userId, Long productId) {
        // Validate that product exists
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Sản phẩm", productId));

        // Check if already favorited
        if (favoriteRepository.existsByUser_IdAndProduct_Id(userId, productId)) {
            throw new BadRequestException("Sản phẩm đã có trong danh sách yêu thích");
        }

        // Get user
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", userId));

        // Create and save favorite
        Favorite favorite = Favorite.builder()
                .user(user)
                .product(product)
                .build();

        favoriteRepository.save(favorite);
        log.info("Product {} added to favorites for user {}", productId, userId);
    }

    /**
     * Remove a product from user's favorites
     */
    @Transactional
    public void removeFavorite(Long userId, Long productId) {
        // Check if favorite exists
        if (!favoriteRepository.existsByUser_IdAndProduct_Id(userId, productId)) {
            throw new ResourceNotFoundException("Không tìm thấy sản phẩm trong danh sách yêu thích");
        }

        // Delete the favorite
        favoriteRepository.deleteByUser_IdAndProduct_Id(userId, productId);
        log.info("Product {} removed from favorites for user {}", productId, userId);
    }

    /**
     * Check if a product is in user's favorites
     */
    @Transactional(readOnly = true)
    public boolean isFavorited(Long userId, Long productId) {
        return favoriteRepository.existsByUser_IdAndProduct_Id(userId, productId);
    }

    /**
     * Get products user is currently bidding on
     * Returns products where user has placed bids and auction is still active
     */
    @Transactional(readOnly = true)
    public Page<BiddingProductResponse> getBiddingProducts(Long userId, Pageable pageable) {
        Page<BidHistory> bidHistories = bidHistoryRepository.findByBidder_IdAndProduct_IsEndedFalse(userId,
                pageable);

        return bidHistories.map(bidHistory -> {
            Product product = bidHistory.getProduct();
            String thumbnailUrl = product.getImages().isEmpty() ? null
                    : product.getImages().get(0).getUrl();

            // Get user's highest bid for this product
            var userHighestBid = bidHistoryRepository.findTopByProductIdAndBidderIdOrderByMaxBidAmountDesc(
                    product.getId(), userId)
                    .map(BidHistory::getMaxBidAmount)
                    .orElse(bidHistory.getMaxBidAmount());

            boolean isWinning = product.getHighestBidder() != null
                    && product.getHighestBidder().getId().equals(userId);

            return BiddingProductResponse.builder()
                    .productId(product.getId())
                    .title(product.getTitle())
                    .slug(product.getSlug())
                    .thumbnailUrl(thumbnailUrl)
                    .currentPrice(product.getCurrentPrice())
                    .userHighestBid(userHighestBid)
                    .isWinning(isWinning)
                    .bidCount(product.getBidCount())
                    .endTime(product.getEndTime())
                    .isEnded(product.getIsEnded())
                    .build();
        });
    }

    /**
     * Get products user has won
     * Returns products where user is the winner and auction has ended
     */
    @Transactional(readOnly = true)
    public Page<WonProductResponse> getWonProducts(Long userId, Pageable pageable) {
        Page<Product> wonProducts = productRepository.findByWinner_IdAndIsEndedTrue(userId, pageable);

        return wonProducts.map(product -> {
            String thumbnailUrl = product.getImages().isEmpty() ? null
                    : product.getImages().get(0).getUrl();

            // Check if user has reviewed this product
            boolean hasReviewed = reviewRepository.existsByProduct_IdAndReviewer_Id(product.getId(), userId);

            return WonProductResponse.builder()
                    .productId(product.getId())
                    .title(product.getTitle())
                    .slug(product.getSlug())
                    .thumbnailUrl(thumbnailUrl)
                    .winningBid(product.getCurrentPrice())
                    .sellerId(product.getSeller().getId())
                    .sellerName(product.getSeller().getFullName())
                    .endTime(product.getEndTime())
                    .hasReviewed(hasReviewed)
                    .build();
        });
    }

    /**
     * Get seller's active products (not ended)
     */
    @Transactional(readOnly = true)
    public Page<SellerActiveProductResponse> getActiveSellerProducts(Long sellerId, Pageable pageable) {
        Page<Product> products = productRepository.findBySeller_IdAndIsEndedFalseOrderByCreatedAtDesc(sellerId,
                pageable);

        return products.map(product -> {
            String thumbnailUrl = product.getImages().stream()
                    .filter(image -> image.getIsPrimary())
                    .findFirst()
                    .map(ProductImage::getUrl)
                    .orElse(null);

            return SellerActiveProductResponse.builder()
                    .id(product.getId())
                    .title(product.getTitle())
                    .slug(product.getSlug())
                    .thumbnailUrl(thumbnailUrl)
                    .startingPrice(product.getStartingPrice())
                    .currentPrice(product.getCurrentPrice())
                    .buyNowPrice(product.getBuyNowPrice())
                    .bidCount(product.getBidCount())
                    .endTime(product.getEndTime())
                    .createdAt(product.getCreatedAt())
                    .categoryName(product.getCategory().getName())
                    .categorySlug(product.getCategory().getSlug())
                    .build();
        });
    }

    /**
     * Get seller's ended products with winners
     * Returns detailed information including winner details and review status
     */
    @Transactional(readOnly = true)
    public Page<SellerEndedProductResponse> getEndedProductsWithWinners(Long sellerId, Pageable pageable) {
        Page<Product> products = productRepository.findBySeller_IdAndIsEndedTrueAndWinnerIsNotNull(sellerId,
                pageable);

        return products.map(product -> {
            String thumbnailUrl = product.getImages().stream()
                    .filter(image -> image.getIsPrimary())
                    .findFirst()
                    .map(ProductImage::getUrl)
                    .orElse(null);

            // Check if seller has reviewed this winner
            boolean hasReviewed = reviewRepository.existsByProduct_IdAndReviewer_Id(
                    product.getId(),
                    sellerId);

            // Get order status and ID if order exists
            Optional<OrderCompletion> orderCompletion = orderCompletionRepository.findByProduct_Id(product.getId());
            String orderStatus = orderCompletion
                    .map(order -> order.getStatus().name())
                    .orElse(null);
            Long orderId = orderCompletion
                    .map(order -> order.getId())
                    .orElse(null);

            return SellerEndedProductResponse.builder()
                    .productId(product.getId())
                    .title(product.getTitle())
                    .slug(product.getSlug())
                    .thumbnailUrl(thumbnailUrl)
                    .startingPrice(product.getStartingPrice())
                    .finalPrice(product.getCurrentPrice())
                    .endTime(product.getEndTime())
                    .winnerId(product.getWinner().getId())
                    .winnerName(product.getWinner().getFullName())
                    .winnerPositiveRating(product.getWinner().getPositiveRating())
                    .winnerNegativeRating(product.getWinner().getNegativeRating())
                    .hasReviewed(hasReviewed)
                    .orderStatus(orderStatus)
                    .orderId(orderId)
                    .build();
        });
    }

    /**
     * Upload user avatar
     * If user already has an avatar, it will be deleted from S3 before uploading
     * the new one
     */
    @Transactional
    public UserProfileResponse uploadAvatar(Long userId, MultipartFile file) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", userId));

        String oldAvatarUrl = user.getAvatarUrl();

        // Upload new avatar
        String avatarUrl = s3Service.uploadAvatar(file, "avatars");
        user.setAvatarUrl(avatarUrl);
        userRepository.save(user);

        // Delete old avatar from S3 if exists
        if (oldAvatarUrl != null && !oldAvatarUrl.isEmpty()) {
            try {
                s3Service.deleteFile(oldAvatarUrl);
                log.info("Deleted old avatar for user: {}", userId);
            } catch (Exception e) {
                log.warn("Failed to delete old avatar for user {}: {}", userId, e.getMessage());
            }
        }

        log.info("Avatar uploaded successfully for user: {}", userId);
        return getUserProfile(userId);
    }

    /**
     * Delete user avatar
     */
    @Transactional
    public UserProfileResponse deleteAvatar(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", userId));

        if (user.getAvatarUrl() == null || user.getAvatarUrl().isEmpty()) {
            throw new BadRequestException("Bạn chưa có ảnh đại diện");
        }

        // Delete from S3
        try {
            s3Service.deleteFile(user.getAvatarUrl());
        } catch (Exception e) {
            log.error("Failed to delete avatar from S3 for user {}: {}", userId, e.getMessage());
            throw new BadRequestException("Lỗi khi xóa ảnh đại diện: " + e.getMessage());
        }

        // Update user record
        user.setAvatarUrl("https://bidstorm.s3.ap-southeast-2.amazonaws.com/avatar.png");
        userRepository.save(user);

        log.info("Avatar deleted successfully for user: {}", userId);
        return getUserProfile(userId);
    }
}
