package com.taitrinh.online_auction.dto.profile;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Basic user information for reviewee (person being reviewed)")
public class RevieweeProfileResponse {

    @Schema(description = "User ID", example = "1")
    private Long id;

    @Schema(description = "User full name", example = "Nguyễn Văn A")
    private String fullName;

    @Schema(description = "User avatar URL", example = "https://example.com/avatars/1.jpg")
    private String avatarUrl;

    @Schema(description = "Positive rating count", example = "10")
    private Integer positiveRating;

    @Schema(description = "Negative rating count", example = "2")
    private Integer negativeRating;

    @Schema(description = "Rating percentage (positive / total)", example = "83.33")
    private Double ratingPercentage;

    @Schema(description = "Total number of ratings", example = "12")
    private Integer totalRatings;
}
