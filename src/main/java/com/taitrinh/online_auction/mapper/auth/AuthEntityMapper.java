package com.taitrinh.online_auction.mapper.auth;

import java.time.ZonedDateTime;

import org.springframework.stereotype.Component;

import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.taitrinh.online_auction.dto.auth.RegisterRequest;
import com.taitrinh.online_auction.entity.auth.EmailOtp;
import com.taitrinh.online_auction.entity.auth.EmailOtp.OtpPurpose;
import com.taitrinh.online_auction.entity.auth.RefreshToken;
import com.taitrinh.online_auction.entity.auth.Role;
import com.taitrinh.online_auction.entity.auth.User;
import com.taitrinh.online_auction.enums.OAuthProvider;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class AuthEntityMapper {

    /**
     * Map RegisterRequest to User entity
     */
    public User toUserFromRegistration(RegisterRequest request, Role role, String passwordHash, String avatarUrl) {
        return User.builder()
                .email(request.getEmail())
                .passwordHash(passwordHash)
                .fullName(request.getFullName())
                .address(request.getAddress())
                .birthDate(request.getBirthDate())
                .avatarUrl(avatarUrl)
                .role(role)
                .emailVerified(false)
                .isActive(true)
                .build();
    }

    /**
     * Map Google OAuth payload to User entity
     */
    public User toUserFromGoogle(GoogleIdToken.Payload payload, Role role, String pictureUrl) {
        String email = payload.getEmail();
        String name = (String) payload.get("name");

        return User.builder()
                .email(email)
                .fullName(name)
                .avatarUrl(pictureUrl)
                .oauthProvider(OAuthProvider.GOOGLE)
                .oauthProviderId(payload.getSubject())
                .emailVerified(true) // Google already verified the email
                .role(role)
                .isActive(true)
                .passwordHash(null) // OAuth users don't have passwords
                .build();
    }

    /**
     * Map token data to RefreshToken entity
     */
    public RefreshToken toRefreshToken(User user, String token, ZonedDateTime expiresAt) {
        return RefreshToken.builder()
                .token(token)
                .user(user)
                .expiresAt(expiresAt)
                .build();
    }

    /**
     * Map OTP data to EmailOtp entity
     */
    public EmailOtp toEmailOtp(String email, String otpCode, OtpPurpose purpose, ZonedDateTime expiresAt) {
        return EmailOtp.builder()
                .email(email)
                .otpCode(otpCode)
                .purpose(purpose)
                .isUsed(false)
                .expiresAt(expiresAt)
                .build();
    }
}
