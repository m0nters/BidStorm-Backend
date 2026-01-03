package com.taitrinh.online_auction.dto.profile;

import java.time.LocalDate;
import java.time.ZonedDateTime;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "User profile information with rating statistics")
public class UserProfileResponse {

    @Schema(description = "User ID", example = "1")
    private Long id;

    @Schema(description = "User email", example = "john.doe@example.com")
    private String email;

    @Schema(description = "User full name", example = "John Doe")
    private String fullName;

    @Schema(description = "User address", example = "123 Main St, City, Country")
    private String address;

    @Schema(description = "User birth date", example = "1990-01-15")
    private LocalDate birthDate;

    @Schema(description = "User role", example = "BIDDER")
    private String role;

    @Schema(description = "User avatar URL", example = "https://bucket.s3.region.amazonaws.com/avatars/xxx.jpg")
    private String avatarUrl;

    @Schema(description = "Number of positive ratings", example = "8")
    private Integer positiveRating;

    @Schema(description = "Number of negative ratings", example = "2")
    private Integer negativeRating;

    @Schema(description = "Rating percentage (0-100)", example = "80.0")
    private Double ratingPercentage;

    @Schema(description = "Total number of ratings", example = "10")
    private Integer totalRatings;

    @Schema(description = "Whether email is verified", example = "true")
    private Boolean emailVerified;

    @Schema(description = "Whether account is active", example = "true")
    private Boolean isActive;

    @Schema(description = "Account creation date")
    private ZonedDateTime createdAt;
}
