package com.taitrinh.online_auction.mapper.auth;

import org.springframework.stereotype.Component;

import com.taitrinh.online_auction.dto.auth.AuthResponse;
import com.taitrinh.online_auction.dto.auth.LoginResponse;
import com.taitrinh.online_auction.entity.auth.User;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class AuthResponseMapper {

    /**
     * Map User entity and tokens to LoginResponse DTO
     */
    public LoginResponse toLoginResponse(User user, String accessToken, String refreshToken,
            Long accessTokenExpiration) {
        return LoginResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .tokenType("Bearer")
                .expiresIn(accessTokenExpiration / 1000) // Convert to seconds
                .user(toUserInfo(user))
                .build();
    }

    /**
     * Map User entity and tokens to AuthResponse DTO (for OAuth)
     */
    public AuthResponse toAuthResponse(User user, String accessToken, String refreshToken) {
        return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .tokenType("Bearer")
                .user(toUserInfo(user))
                .build();
    }

    /**
     * Map User entity to UserInfo DTO (shared logic)
     */
    public LoginResponse.UserInfo toUserInfo(User user) {
        return LoginResponse.UserInfo.builder()
                .id(user.getId())
                .email(user.getEmail())
                .fullName(user.getFullName())
                .role(user.getRole().getName())
                .avatarUrl(user.getAvatarUrl())
                .emailVerified(user.getEmailVerified())
                .isActive(user.getIsActive())
                .build();
    }
}
