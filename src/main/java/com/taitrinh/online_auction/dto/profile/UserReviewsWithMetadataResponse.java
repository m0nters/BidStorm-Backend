package com.taitrinh.online_auction.dto.profile;

import org.springframework.data.domain.Page;

import com.taitrinh.online_auction.entity.User;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "User reviews with metadata")
public class UserReviewsWithMetadataResponse {

    @Schema(description = "Number of positive ratings", example = "45")
    private Integer positiveRating;

    @Schema(description = "Number of negative ratings", example = "5")
    private Integer negativeRating;

    @Schema(description = "Rating percentage (0-100)", example = "90.0")
    private Double ratingPercentage;

    @Schema(description = "Total number of ratings", example = "50")
    private Integer totalRatings;

    @Schema(description = "Paginated list of reviews")
    private Page<ReviewResponse> reviews;

    /**
     * Helper method to create response from user and reviews
     */
    public static UserReviewsWithMetadataResponse from(User user,
            Page<ReviewResponse> reviews) {
        return UserReviewsWithMetadataResponse.builder()
                .positiveRating(user.getPositiveRating())
                .negativeRating(user.getNegativeRating())
                .ratingPercentage(user.getRatingPercentage())
                .totalRatings(user.getPositiveRating() + user.getNegativeRating())
                .reviews(reviews)
                .build();
    }
}
