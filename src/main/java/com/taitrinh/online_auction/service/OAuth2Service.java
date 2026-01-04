package com.taitrinh.online_auction.service;

import java.time.Duration;
import java.time.ZonedDateTime;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.taitrinh.online_auction.dto.auth.AuthResponse;
import com.taitrinh.online_auction.dto.auth.LoginResponse;
import com.taitrinh.online_auction.entity.RefreshToken;
import com.taitrinh.online_auction.entity.Role;
import com.taitrinh.online_auction.entity.User;
import com.taitrinh.online_auction.enums.OAuthProvider;
import com.taitrinh.online_auction.exception.BadRequestException;
import com.taitrinh.online_auction.exception.ResourceNotFoundException;
import com.taitrinh.online_auction.repository.RefreshTokenRepository;
import com.taitrinh.online_auction.repository.RoleRepository;
import com.taitrinh.online_auction.repository.UserRepository;
import com.taitrinh.online_auction.security.JwtUtil;
import com.taitrinh.online_auction.security.UserDetailsImpl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class OAuth2Service {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final JwtUtil jwtUtil;

    @Value("${google.oauth.client-id}")
    private String googleClientId;

    @Value("${jwt.refresh-token-expiration}")
    private Long refreshTokenExpiration;

    @Value("${app.default-avatar-url}")
    private String defaultAvatarUrl;

    /**
     * Authenticate user with Google One Tap (JWT credential verification)
     * 
     * @param credential JWT ID token from Google One Tap
     * @return AuthResponse with JWT tokens
     */
    @Transactional
    public AuthResponse authenticateWithGoogle(String credential) {
        log.info("Starting Google One Tap authentication");

        // Step 1: Verify JWT credential and extract user info
        GoogleIdToken.Payload payload = verifyGoogleToken(credential);
        log.info("Successfully verified Google token for email: {}", payload.getEmail());

        // Step 2: Validate email is verified
        if (!Boolean.TRUE.equals(payload.getEmailVerified())) {
            log.error("Google email not verified: {}", payload.getEmail());
            throw new BadRequestException("Email Google chưa được xác minh");
        }

        // Step 3: Find or create user
        User user = findOrCreateUser(payload);
        log.info("User authenticated: {} (ID: {})", user.getEmail(), user.getId());

        // Step 4: Create UserDetails for JWT generation
        UserDetailsImpl userDetails = new UserDetailsImpl(user);

        // Step 5: Generate JWT tokens
        String accessToken = jwtUtil.generateAccessToken(userDetails, user.getId(), user.getRole().getName());
        String refreshToken = jwtUtil.generateRefreshToken(userDetails, user.getId());

        // Step 6: Save refresh token
        RefreshToken refreshTokenEntity = RefreshToken.builder()
                .user(user)
                .token(refreshToken)
                .expiresAt(ZonedDateTime.now().plus(Duration.ofMillis(refreshTokenExpiration)))
                .build();
        refreshTokenRepository.save(refreshTokenEntity);

        // Step 7: Build response with UserInfo
        LoginResponse.UserInfo userInfoResponse = LoginResponse.UserInfo.builder()
                .id(user.getId())
                .email(user.getEmail())
                .fullName(user.getFullName())
                .role(user.getRole().getName())
                .avatarUrl(user.getAvatarUrl())
                .emailVerified(user.getEmailVerified())
                .isActive(user.getIsActive())
                .build();

        return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .tokenType("Bearer")
                .user(userInfoResponse)
                .build();
    }

    /**
     * Verify Google JWT token and extract payload
     */
    private GoogleIdToken.Payload verifyGoogleToken(String credential) {
        try {
            GoogleIdTokenVerifier verifier = new GoogleIdTokenVerifier.Builder(
                    new NetHttpTransport(),
                    GsonFactory.getDefaultInstance())
                    .setAudience(java.util.Collections.singletonList(googleClientId))
                    .build();

            GoogleIdToken idToken = verifier.verify(credential);

            if (idToken == null) {
                throw new BadRequestException("Token Google không hợp lệ");
            }

            return idToken.getPayload();

        } catch (Exception e) {
            log.error("Error verifying Google token: {}", e.getMessage(), e);
            throw new BadRequestException("Xác thực Google thất bại: " + e.getMessage());
        }
    }

    /**
     * Find existing user by email or create new user from Google token payload
     * 
     * @param payload Google token payload
     * @return User entity (existing or newly created)
     */
    private User findOrCreateUser(GoogleIdToken.Payload payload) {
        String email = payload.getEmail();

        // Try to find existing user by email
        return userRepository.findByEmail(email)
                .map(existingUser -> updateExistingUserWithGoogle(existingUser, payload))
                .orElseGet(() -> createNewUserFromGoogle(payload));
    }

    /**
     * Update existing user with Google OAuth info
     */
    private User updateExistingUserWithGoogle(User user, GoogleIdToken.Payload payload) {
        log.info("Updating existing user {} with Google OAuth info", user.getEmail());

        // Update OAuth fields if not already set
        if (user.getOauthProvider() == OAuthProvider.LOCAL) {
            user.setOauthProvider(OAuthProvider.GOOGLE);
            user.setOauthProviderId(payload.getSubject());
        }

        // Update avatar if user is still using default avatar and Google provides one
        String pictureUrl = (String) payload.get("picture");
        if ((user.getAvatarUrl() == null || user.getAvatarUrl().equals(defaultAvatarUrl))
                && pictureUrl != null) {
            user.setAvatarUrl(pictureUrl);
            log.info("Updated user avatar from Google profile picture");
        }

        // Mark email as verified (since Google verified it)
        if (!user.getEmailVerified()) {
            user.setEmailVerified(true);
            log.info("Marked email as verified from Google");
        }

        return userRepository.save(user);
    }

    /**
     * Create new user from Google token payload
     */
    private User createNewUserFromGoogle(GoogleIdToken.Payload payload) {
        log.info("Creating new user from Google One Tap: {}", payload.getEmail());

        // Get default bidder role
        Role bidderRole = roleRepository.findById(Role.BIDDER)
                .orElseThrow(() -> new ResourceNotFoundException("Role không tồn tại"));

        // Extract user info from payload
        String email = payload.getEmail(); // always NOT NULL
        String name = (String) payload.get("name"); // always NOT NULL
        String pictureUrl = (String) payload.get("picture"); // always NOT NULL (even if the user hasn't uploaded a
                                                             // profile picture before, Google provides a default one)

        // Create new user
        User newUser = User.builder()
                .email(email)
                .fullName(name)
                .avatarUrl(pictureUrl)
                .oauthProvider(OAuthProvider.GOOGLE)
                .oauthProviderId(payload.getSubject())
                .emailVerified(true) // Google already verified the email
                .role(bidderRole)
                .isActive(true)
                .passwordHash(null) // OAuth users don't have passwords
                .build();

        User savedUser = userRepository.save(newUser);
        log.info("Created new user with ID: {}", savedUser.getId());

        return savedUser;
    }
}
