package com.taitrinh.online_auction.dto.auth;

import com.fasterxml.jackson.annotation.JsonInclude;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Login response with JWT tokens")
public class LoginResponse {

    @Schema(description = "JWT access token (short-lived)", example = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...")
    private String accessToken;

    @Schema(description = "JWT refresh token (long-lived)", example = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...")
    private String refreshToken;

    @Schema(description = "Token type", example = "Bearer")
    @Builder.Default
    private String tokenType = "Bearer";

    @Schema(description = "Access token expiration time in seconds", example = "3600")
    private Long expiresIn;

    @Schema(description = "User information")
    private UserInfo user;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "User information")
    public static class UserInfo {
        @Schema(description = "User ID", example = "1")
        private Long id;

        @Schema(description = "User email", example = "john.doe@example.com")
        private String email;

        @Schema(description = "User full name", example = "John Doe")
        private String fullName;

        @Schema(description = "User role", example = "BIDDER")
        private String role;

        @Schema(description = "User avatar URL", example = "https://bucket.s3.region.amazonaws.com/avatars/xxx.jpg")
        private String avatarUrl;

        @Schema(description = "Email verified status", example = "true")
        private Boolean emailVerified;

        @Schema(description = "Account active status", example = "true")
        private Boolean isActive;
    }
}
