package com.taitrinh.online_auction.service.auth;

import java.security.SecureRandom;
import java.time.Duration;
import java.time.ZonedDateTime;
import java.util.Random;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.taitrinh.online_auction.dto.auth.LoginRequest;
import com.taitrinh.online_auction.dto.auth.LoginResponse;
import com.taitrinh.online_auction.dto.auth.OtpRequest;
import com.taitrinh.online_auction.dto.auth.RegisterRequest;
import com.taitrinh.online_auction.entity.auth.EmailOtp;
import com.taitrinh.online_auction.entity.auth.EmailOtp.OtpPurpose;
import com.taitrinh.online_auction.entity.auth.RefreshToken;
import com.taitrinh.online_auction.entity.auth.Role;
import com.taitrinh.online_auction.entity.auth.User;
import com.taitrinh.online_auction.exception.auth.AccountInactiveException;
import com.taitrinh.online_auction.exception.auth.EmailAlreadyExistsException;
import com.taitrinh.online_auction.exception.auth.EmailAlreadyVerifiedException;
import com.taitrinh.online_auction.exception.auth.InvalidOtpException;
import com.taitrinh.online_auction.exception.auth.InvalidRefreshTokenException;
import com.taitrinh.online_auction.exception.auth.OtpRateLimitException;
import com.taitrinh.online_auction.exception.common.ResourceNotFoundException;
import com.taitrinh.online_auction.mapper.auth.AuthEntityMapper;
import com.taitrinh.online_auction.mapper.auth.AuthResponseMapper;
import com.taitrinh.online_auction.repository.auth.EmailOtpRepository;
import com.taitrinh.online_auction.repository.auth.RefreshTokenRepository;
import com.taitrinh.online_auction.repository.auth.RoleRepository;
import com.taitrinh.online_auction.repository.auth.UserRepository;
import com.taitrinh.online_auction.security.JwtUtil;
import com.taitrinh.online_auction.security.UserDetailsImpl;
import com.taitrinh.online_auction.service.email.AuthEmailService;
import com.taitrinh.online_auction.service.integration.RecaptchaService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final EmailOtpRepository emailOtpRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final AuthenticationManager authenticationManager;
    private final RecaptchaService recaptchaService;
    private final AuthEmailService authEmailService;
    private final ApplicationContext applicationContext;
    private final AuthResponseMapper authResponseMapper;
    private final AuthEntityMapper authEntityMapper;

    @Value("${jwt.access-token-expiration}")
    private Long accessTokenExpiration;

    @Value("${jwt.refresh-token-expiration}")
    private Long refreshTokenExpiration;

    @Value("${otp.expiration-minutes:10}")
    private Integer otpExpirationMinutes;

    @Value("${app.default-avatar-url}")
    private String defaultAvatarUrl;

    private final Random random = new SecureRandom();

    @Transactional
    public void register(RegisterRequest request) {
        // Verify reCAPTCHA token
        recaptchaService.verifyRecaptcha(request.getRecaptchaToken());

        // Check if email already exists
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new EmailAlreadyExistsException(request.getEmail());
        }

        // Get default "BIDDER" role (id = 3)
        Role bidderRole = roleRepository.findById((short) 3)
                .orElseThrow(() -> new ResourceNotFoundException("Role", 3));

        // Create user
        String passwordHash = passwordEncoder.encode(request.getPassword());
        User user = authEntityMapper.toUserFromRegistration(request, bidderRole, passwordHash, defaultAvatarUrl);

        userRepository.save(user);

        // Generate and send OTP
        sendOtp(request.getEmail(), OtpPurpose.EMAIL_VERIFICATION);

        log.info("User registered successfully: {}", request.getEmail());
    }

    @Transactional
    public LoginResponse login(LoginRequest request) {
        // Authenticate user
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword()));

        UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();
        User user = userDetails.getUser();

        // Check if user is active
        if (!user.getIsActive()) {
            throw new AccountInactiveException();
        }

        // Generate tokens
        String accessToken = jwtUtil.generateAccessToken(user.getEmail(), user.getId());
        String refreshToken = jwtUtil.generateRefreshToken(user.getEmail(), user.getId());

        // Save refresh token to database
        saveRefreshToken(user, refreshToken);

        // Build response
        return authResponseMapper.toLoginResponse(user, accessToken, refreshToken, accessTokenExpiration);
    }

    @Transactional
    public void forgotPassword(String email) {
        // Check if user exists (but don't reveal if they don't - security best
        // practice)
        if (!userRepository.existsByEmail(email)) {
            log.warn("Password reset requested for non-existent email: {}", email);
            // Still return success to prevent email enumeration attacks
            return;
        }

        // Generate and send OTP
        sendOtp(email, OtpPurpose.PASSWORD_RESET);

        log.info("Password reset OTP sent to: {}", email);
    }

    @Transactional
    public void verifyResetPasswordOtp(String email, String otpCode) {
        // Find and verify OTP
        EmailOtp otp = emailOtpRepository.findValidOtp(
                email,
                otpCode,
                OtpPurpose.PASSWORD_RESET,
                ZonedDateTime.now())
                .orElseThrow(() -> new InvalidOtpException());

        // Mark OTP as used
        otp.setIsUsed(true);
        emailOtpRepository.save(otp);

        log.info("Password reset OTP verified and marked as used for: {}", email);
    }

    @Transactional
    public void resetPassword(String email, String newPassword) {
        // Find user (OTP was already verified in previous step)
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User", email));

        // Update password
        user.setPasswordHash(passwordEncoder.encode(newPassword));
        userRepository.save(user);

        // Revoke all refresh tokens for security (force re-login for every session)
        refreshTokenRepository.revokeAllByUserId(user.getId(), ZonedDateTime.now());

        log.info("Password reset successfully for user: {}. All tokens revoked.", email);
    }

    @Transactional
    public LoginResponse refreshToken(String refreshToken) {
        // Validate refresh token format
        if (!jwtUtil.validateRefreshToken(refreshToken)) {
            throw new InvalidRefreshTokenException();
        }

        // Extract user email from token
        String email = jwtUtil.extractUsername(refreshToken);

        // Verify token exists in database
        RefreshToken storedToken = refreshTokenRepository.findByToken(refreshToken)
                .orElseThrow(() -> new InvalidRefreshTokenException("Không tìm thấy refresh token"));

        // SECURITY: Check if token has been revoked (possible reuse attack)
        if (storedToken.isRevoked()) {
            log.warn("⚠️ SECURITY ALERT: Revoked token reuse detected for user: {}. Revoking all tokens.", email);

            // Revoke all tokens in SEPARATE transaction (persists even when exception is
            // thrown)
            // Call through Spring proxy to ensure REQUIRES_NEW propagation works
            applicationContext.getBean(AuthService.class).revokeAllUserTokens(storedToken.getUser().getId());
            throw new InvalidRefreshTokenException("Token đã bị thu hồi. Vui lòng đăng nhập lại.");
        }

        // Check if token is expired
        if (storedToken.isExpired()) {
            refreshTokenRepository.delete(storedToken);
            throw new InvalidRefreshTokenException("Refresh token đã hết hạn");
        }

        // Get user
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User", email));

        // Generate new tokens
        String newAccessToken = jwtUtil.generateAccessToken(user.getEmail(), user.getId());
        String newRefreshToken = jwtUtil.generateRefreshToken(user.getEmail(), user.getId());

        // TOKEN ROTATION: Revoke old token and link to new one
        storedToken.revokeAndReplace(newRefreshToken);
        refreshTokenRepository.save(storedToken);
        refreshTokenRepository.flush(); // Force immediate persistence to database

        log.info("Token rotated for user: {}. Old token revoked.", email);

        // Save new refresh token
        ZonedDateTime newTokenExpiresAt = ZonedDateTime.now().plusNanos(refreshTokenExpiration * 1_000_000);
        RefreshToken newToken = authEntityMapper.toRefreshToken(user, newRefreshToken, newTokenExpiresAt);
        refreshTokenRepository.save(newToken);

        // Build response with NEW refresh token
        return authResponseMapper.toLoginResponse(user, newAccessToken, newRefreshToken, accessTokenExpiration);
    }

    @Transactional
    public void verifyEmailOtp(OtpRequest request) {
        // Find valid OTP
        EmailOtp otp = emailOtpRepository.findValidOtp(
                request.getEmail(),
                request.getOtpCode(),
                OtpPurpose.EMAIL_VERIFICATION,
                ZonedDateTime.now())
                .orElseThrow(() -> new InvalidOtpException());

        // Mark OTP as used
        otp.setIsUsed(true);
        emailOtpRepository.save(otp);

        // Update user email verification status
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new ResourceNotFoundException("User", request.getEmail()));

        user.setEmailVerified(true);
        userRepository.save(user);

        log.info("Email verified successfully: {}", request.getEmail());

        // Send welcome email
        authEmailService.sendWelcomeEmail(user.getEmail(), user.getFullName());
        log.info("Welcome email sent to: {}", user.getEmail());
    }

    @Transactional
    public void resendEmailVerificationOtp(String email) {
        // Check if user exists
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User", email));

        // Check if email is already verified
        if (user.getEmailVerified()) {
            throw new EmailAlreadyVerifiedException(email);
        }

        // Generate and send new OTP
        sendOtp(email, OtpPurpose.EMAIL_VERIFICATION);

        log.info("OTP resent to: {}", email);
    }

    private void sendOtp(String email, OtpPurpose purpose) {
        // Rate limiting: Check if last OTP was sent within 1 minute
        emailOtpRepository.findFirstByEmailAndPurposeOrderByCreatedAtDesc(email, purpose)
                .ifPresent(lastOtp -> {
                    ZonedDateTime lastSentAt = lastOtp.getCreatedAt();
                    ZonedDateTime oneMinuteAgo = ZonedDateTime.now().minusMinutes(1);

                    if (lastSentAt.isAfter(oneMinuteAgo)) {
                        long secondsRemaining = Duration.between(ZonedDateTime.now(), lastSentAt.plusMinutes(1))
                                .getSeconds();
                        throw new OtpRateLimitException(
                                String.format("Vui lòng đợi %d giây nữa trước khi yêu cầu OTP khác",
                                        secondsRemaining));
                    }
                });

        // Mark all previous OTPs as used (security best practice)
        emailOtpRepository.markAllAsUsed(email, purpose);

        // Generate 6-digit OTP
        String otpCode = String.format("%06d", random.nextInt(1000000));

        // Save OTP to database
        ZonedDateTime otpExpiresAt = ZonedDateTime.now().plusMinutes(otpExpirationMinutes);
        EmailOtp otp = authEntityMapper.toEmailOtp(email, otpCode, purpose, otpExpiresAt);

        emailOtpRepository.save(otp);

        // Send email with OTP (different template based on purpose)
        if (purpose == OtpPurpose.PASSWORD_RESET) {
            authEmailService.sendPasswordResetOtp(email, otpCode, otpExpirationMinutes);
        } else {
            authEmailService.sendEmailVerificationOTP(email, otpCode, otpExpirationMinutes);
        }

        log.info("OTP sent to: {}", email);
    }

    private void saveRefreshToken(User user, String token) {
        // Revoke existing active refresh tokens for this user (audit trail)
        refreshTokenRepository.revokeAllByUserId(user.getId(), ZonedDateTime.now());
        log.info("Revoked existing refresh tokens for user: {}", user.getId());

        // Save new refresh token
        ZonedDateTime expiresAt = ZonedDateTime.now().plusNanos(refreshTokenExpiration * 1_000_000);
        RefreshToken refreshToken = authEntityMapper.toRefreshToken(user, token, expiresAt);
        refreshTokenRepository.save(refreshToken);
        log.info("Saved new refresh token for user: {}", user.getId());
    }

    /**
     * Revoke all refresh tokens for a user in a SEPARATE transaction.
     * This ensures the revocation persists even if the calling method throws an
     * exception.
     * Used for security breach detection.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void revokeAllUserTokens(Long userId) {
        refreshTokenRepository.revokeAllByUserId(userId, ZonedDateTime.now());
        log.info("✅ All tokens revoked for user {} due to security breach", userId);
    }

    @Transactional
    public void logout(Long userId) {
        // Revoke all tokens instead of deleting (maintains audit trail)
        refreshTokenRepository.revokeAllByUserId(userId, ZonedDateTime.now());
        log.info("Revoked all refresh tokens for user {} during logout", userId);
    }
}
