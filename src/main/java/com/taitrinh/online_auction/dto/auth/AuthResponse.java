package com.taitrinh.online_auction.dto.auth;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.taitrinh.online_auction.dto.auth.LoginResponse.UserInfo;

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
@Schema(description = "Authentication response with JWT tokens (used for OAuth and standard auth)")
public class AuthResponse {

    @Schema(description = "JWT access token (short-lived)", example = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...")
    private String accessToken;

    @Schema(description = "JWT refresh token (long-lived)", example = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...")
    private String refreshToken;

    @Schema(description = "Token type", example = "Bearer")
    @Builder.Default
    private String tokenType = "Bearer";

    @Schema(description = "User information")
    private UserInfo user;
}
