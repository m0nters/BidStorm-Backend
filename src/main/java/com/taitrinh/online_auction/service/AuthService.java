package com.taitrinh.online_auction.service;

import java.security.SecureRandom;
import java.time.ZonedDateTime;
import java.util.Random;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.taitrinh.online_auction.dto.auth.LoginRequest;
import com.taitrinh.online_auction.dto.auth.LoginResponse;
import com.taitrinh.online_auction.dto.auth.RegisterRequest;
import com.taitrinh.online_auction.dto.auth.VerifyOtpRequest;
import com.taitrinh.online_auction.entity.EmailOtp;
import com.taitrinh.online_auction.entity.EmailOtp.OtpPurpose;
import com.taitrinh.online_auction.entity.RefreshToken;
import com.taitrinh.online_auction.entity.Role;
import com.taitrinh.online_auction.entity.User;
import com.taitrinh.online_auction.exception.AccountInactiveException;
import com.taitrinh.online_auction.exception.EmailAlreadyExistsException;
import com.taitrinh.online_auction.exception.EmailAlreadyVerifiedException;
import com.taitrinh.online_auction.exception.InvalidOtpException;
import com.taitrinh.online_auction.exception.InvalidRefreshTokenException;
import com.taitrinh.online_auction.exception.ResourceNotFoundException;
import com.taitrinh.online_auction.repository.EmailOtpRepository;
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
public class AuthService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final EmailOtpRepository emailOtpRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final AuthenticationManager authenticationManager;

    @Value("${jwt.access-token-expiration}")
    private Long accessTokenExpiration;

    @Value("${jwt.refresh-token-expiration}")
    private Long refreshTokenExpiration;

    @Value("${otp.expiration-minutes:10}")
    private Integer otpExpirationMinutes;

    private final Random random = new SecureRandom();

    @Transactional
    public void register(RegisterRequest request) {
        // TODO: Verify reCAPTCHA token
        // This requires calling Google reCAPTCHA API
        // For now, we'll skip this validation

        // Check if email already exists
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new EmailAlreadyExistsException(request.getEmail());
        }

        // Get BIDDER role (default role for new users)
        Role bidderRole = roleRepository.findById(Role.BIDDER)
                .orElseThrow(() -> new IllegalStateException("BIDDER role not found"));

        // Create user
        User user = User.builder()
                .email(request.getEmail())
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .fullName(request.getFullName())
                .address(request.getAddress())
                .birthDate(request.getBirthDate())
                .role(bidderRole)
                .emailVerified(false)
                .isActive(true)
                .build();

        userRepository.save(user);

        // Generate and send OTP
        sendOtp(request.getEmail(), OtpPurpose.REGISTRATION);

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
        String accessToken = jwtUtil.generateAccessToken(userDetails, user.getId(), user.getRole().getName());
        String refreshToken = jwtUtil.generateRefreshToken(userDetails, user.getId());

        // Save refresh token to database
        saveRefreshToken(user, refreshToken);

        // Build response
        return LoginResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .tokenType("Bearer")
                .expiresIn(accessTokenExpiration / 1000) // Convert to seconds
                .user(LoginResponse.UserInfo.builder()
                        .id(user.getId())
                        .email(user.getEmail())
                        .fullName(user.getFullName())
                        .role(user.getRole().getName())
                        .emailVerified(user.getEmailVerified())
                        .isActive(user.getIsActive())
                        .build())
                .build();
    }

    @Transactional
    public LoginResponse refreshToken(String refreshToken) {
        // Validate refresh token format
        if (!jwtUtil.validateToken(refreshToken)) {
            throw new InvalidRefreshTokenException();
        }

        // Extract user email from token
        String email = jwtUtil.extractUsername(refreshToken);

        // Verify token exists in database
        RefreshToken storedToken = refreshTokenRepository.findByToken(refreshToken)
                .orElseThrow(() -> new InvalidRefreshTokenException("Refresh token not found"));

        // Check if token is expired
        if (storedToken.getExpiresAt().isBefore(ZonedDateTime.now())) {
            refreshTokenRepository.delete(storedToken);
            throw new InvalidRefreshTokenException("Refresh token expired");
        }

        // Get user
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User", email));

        // Generate new access token
        UserDetailsImpl userDetails = new UserDetailsImpl(user);
        String newAccessToken = jwtUtil.generateAccessToken(userDetails, user.getId(), user.getRole().getName());

        // Build response
        return LoginResponse.builder()
                .accessToken(newAccessToken)
                .refreshToken(refreshToken) // Return same refresh token
                .tokenType("Bearer")
                .expiresIn(accessTokenExpiration / 1000)
                .user(LoginResponse.UserInfo.builder()
                        .id(user.getId())
                        .email(user.getEmail())
                        .fullName(user.getFullName())
                        .role(user.getRole().getName())
                        .emailVerified(user.getEmailVerified())
                        .isActive(user.getIsActive())
                        .build())
                .build();
    }

    @Transactional
    public void verifyOtp(VerifyOtpRequest request) {
        // Find valid OTP
        EmailOtp otp = emailOtpRepository.findValidOtp(
                request.getEmail(),
                request.getOtpCode(),
                OtpPurpose.REGISTRATION,
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
    }

    @Transactional
    public void resendOtp(String email) {
        // Check if user exists
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User", email));

        // Check if email is already verified
        if (user.getEmailVerified()) {
            throw new EmailAlreadyVerifiedException(email);
        }

        // Mark all previous OTPs as used
        emailOtpRepository.markAllAsUsed(email, OtpPurpose.REGISTRATION);

        // Generate and send new OTP
        sendOtp(email, OtpPurpose.REGISTRATION);

        log.info("OTP resent to: {}", email);
    }

    private void sendOtp(String email, OtpPurpose purpose) {
        // Generate 6-digit OTP
        String otpCode = String.format("%06d", random.nextInt(1000000));

        // Save OTP to database
        EmailOtp otp = EmailOtp.builder()
                .email(email)
                .otpCode(otpCode)
                .purpose(purpose)
                .isUsed(false)
                .expiresAt(ZonedDateTime.now().plusMinutes(otpExpirationMinutes))
                .build();

        emailOtpRepository.save(otp);

        // TODO: Send email with OTP
        // This requires email service configuration (e.g., SMTP, SendGrid, AWS SES)
        // For now, log the OTP (REMOVE THIS IN PRODUCTION!)
        log.info("OTP for {}: {}", email, otpCode);

        log.info("OTP sent to: {}", email);
    }

    private void saveRefreshToken(User user, String token) {
        // Delete existing refresh token for this user
        refreshTokenRepository.deleteByUserId(user.getId());
        log.info("Delete existing refresh token for user: {}", user.getId());

        // Save new refresh token
        RefreshToken refreshToken = RefreshToken.builder()
                .token(token)
                .user(user)
                .expiresAt(ZonedDateTime.now().plusNanos(refreshTokenExpiration * 1_000_000))
                .build();
        refreshTokenRepository.save(refreshToken);
        log.info("Save new refresh token for user: {}", user.getId());
    }

    @Transactional
    public void logout(Long userId) {
        refreshTokenRepository.deleteByUserId(userId);
        log.info("Delete refresh token when logout for user: {}", userId);
    }
}
