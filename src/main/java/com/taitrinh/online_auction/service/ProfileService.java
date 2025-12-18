package com.taitrinh.online_auction.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.taitrinh.online_auction.dto.profile.BiddingProductResponse;
import com.taitrinh.online_auction.dto.profile.ChangePasswordRequest;
import com.taitrinh.online_auction.dto.profile.CreateReviewRequest;
import com.taitrinh.online_auction.dto.profile.FavoriteProductResponse;
import com.taitrinh.online_auction.dto.profile.ReviewResponse;
import com.taitrinh.online_auction.dto.profile.UpdateProfileRequest;
import com.taitrinh.online_auction.dto.profile.UserProfileResponse;
import com.taitrinh.online_auction.dto.profile.WonProductResponse;
import com.taitrinh.online_auction.entity.BidHistory;
import com.taitrinh.online_auction.entity.Favorite;
import com.taitrinh.online_auction.entity.Product;
import com.taitrinh.online_auction.entity.Review;
import com.taitrinh.online_auction.entity.User;
import com.taitrinh.online_auction.exception.BadRequestException;
import com.taitrinh.online_auction.exception.EmailAlreadyExistsException;
import com.taitrinh.online_auction.exception.ResourceNotFoundException;
import com.taitrinh.online_auction.repository.BidHistoryRepository;
import com.taitrinh.online_auction.repository.FavoriteRepository;
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
    private final PasswordEncoder passwordEncoder;

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

        return reviews.map(review -> ReviewResponse.builder()
                .id(review.getId())
                .productId(review.getProduct().getId())
                .productTitle(review.getProduct().getTitle())
                .reviewerId(review.getReviewer().getId())
                .reviewerName(review.getReviewer().getFullName())
                .rating(review.getRating())
                .comment(review.getComment())
                .createdAt(review.getCreatedAt())
                .build());
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
            var userHighestBid = bidHistoryRepository.findTopByProduct_IdAndBidder_IdOrderByBidAmountDesc(
                    product.getId(), userId)
                    .map(BidHistory::getBidAmount)
                    .orElse(bidHistory.getBidAmount());

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
}
