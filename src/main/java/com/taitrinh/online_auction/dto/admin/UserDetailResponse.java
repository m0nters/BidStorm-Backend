package com.taitrinh.online_auction.dto.admin;

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
@Schema(description = "Detailed user information for admin view")
public class UserDetailResponse {

    @Schema(description = "User ID", example = "1")
    private Long id;

    @Schema(description = "User email", example = "user@example.com")
    private String email;

    @Schema(description = "User full name", example = "Nguyễn Văn A")
    private String fullName;

    @Schema(description = "User avatar URL", example = "https://example.com/avatar.jpg")
    private String avatarUrl;

    @Schema(description = "User address", example = "123 Main St, Hanoi")
    private String address;

    @Schema(description = "Birth date")
    private LocalDate birthDate;

    @Schema(description = "Role ID (1=ADMIN, 2=SELLER, 3=BIDDER)", example = "3")
    private Short roleId;

    @Schema(description = "Role name", example = "BIDDER")
    private String roleName;

    @Schema(description = "Positive rating count", example = "10")
    private Integer positiveRating;

    @Schema(description = "Negative rating count", example = "2")
    private Integer negativeRating;

    @Schema(description = "Email verification status", example = "true")
    private Boolean emailVerified;

    @Schema(description = "Account active status", example = "true")
    private Boolean isActive;

    @Schema(description = "Seller permission expiration (if applicable)")
    private ZonedDateTime sellerExpiresAt;

    @Schema(description = "Admin who upgraded this user (if applicable)")
    private Long sellerUpgradedBy;

    @Schema(description = "Account creation date")
    private ZonedDateTime createdAt;

    @Schema(description = "Last update date")
    private ZonedDateTime updatedAt;
}
