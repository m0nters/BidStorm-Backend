package com.taitrinh.online_auction.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.taitrinh.online_auction.dto.ApiResponse;
import com.taitrinh.online_auction.dto.auth.LoginRequest;
import com.taitrinh.online_auction.dto.auth.LoginResponse;
import com.taitrinh.online_auction.dto.auth.RegisterRequest;
import com.taitrinh.online_auction.dto.auth.VerifyOtpRequest;
import com.taitrinh.online_auction.security.UserDetailsImpl;
import com.taitrinh.online_auction.service.AuthService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@Validated
@Tag(name = "Authentication", description = "User authentication and registration endpoints")
@Slf4j
public class AuthController {

        private final AuthService authService;

        @Value("${jwt.refresh-token-expiration}")
        private Long refreshTokenExpiration;

        @Value("${cookie.secure}")
        private Boolean cookieSecure;

        @Value("${cookie.same-site}")
        private String cookieSameSite;

        @PostMapping("/register")
        @Operation(summary = "Register a new user", description = "Register a new bidder account with email verification. Password must be at least 8 characters with uppercase, lowercase, number, and special character.")
        @ApiResponses(value = {
                        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "User registered successfully. OTP sent to email for verification."),
                        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid input or email already exists")
        })
        public ResponseEntity<ApiResponse<Void>> register(@Valid @RequestBody RegisterRequest request) {
                authService.register(request);
                return ResponseEntity.status(HttpStatus.CREATED).body(
                                ApiResponse.created(null,
                                                "Registration successful. Please check your email for OTP verification."));
        }

        @PostMapping("/login")
        @Operation(summary = "User login", description = "Authenticate user and receive JWT access token. Refresh token is set as httpOnly cookie.")
        @ApiResponses(value = {
                        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Login successful"),
                        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Invalid credentials or account inactive")
        })
        public ResponseEntity<ApiResponse<LoginResponse>> login(@Valid @RequestBody LoginRequest request) {
                LoginResponse response = authService.login(request);

                // Create httpOnly cookie for refresh token
                ResponseCookie refreshTokenCookie = ResponseCookie.from("refreshToken", response.getRefreshToken())
                                .httpOnly(true)
                                .secure(cookieSecure)
                                .path("/")
                                .maxAge(refreshTokenExpiration / 1000) // Convert milliseconds to seconds
                                .sameSite(cookieSameSite)
                                .build();

                // Remove refresh token from response body (it's now in cookie)
                response.setRefreshToken(null);

                return ResponseEntity.ok()
                                .header(HttpHeaders.SET_COOKIE, refreshTokenCookie.toString())
                                .body(ApiResponse.ok(response, "Login successful"));
        }

        @PostMapping("/refresh")
        @Operation(summary = "Refresh access token", description = "Generate a new access token using the refresh token from httpOnly cookie")
        @ApiResponses(value = {
                        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Token refreshed successfully"),
                        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Invalid or expired refresh token")
        })
        public ResponseEntity<ApiResponse<LoginResponse>> refreshToken(
                        @CookieValue(name = "refreshToken", required = true) String refreshToken) {
                LoginResponse response = authService.refreshToken(refreshToken);

                // Refresh token remains in the cookie, so remove it from response body
                response.setRefreshToken(null);

                return ResponseEntity.ok(ApiResponse.ok(response, "Token refreshed successfully"));
        }

        @PostMapping("/verify-otp")
        @Operation(summary = "Verify email with OTP", description = "Verify user email address using the OTP sent during registration")
        @ApiResponses(value = {
                        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Email verified successfully"),
                        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid or expired OTP")
        })
        public ResponseEntity<ApiResponse<Void>> verifyOtp(@Valid @RequestBody VerifyOtpRequest request) {
                authService.verifyOtp(request);
                return ResponseEntity.ok(ApiResponse.ok(null, "Email verified successfully"));
        }

        @PostMapping("/resend-otp")
        @Operation(summary = "Resend OTP", description = "Resend verification OTP to user's email address")
        @ApiResponses(value = {
                        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "OTP sent successfully"),
                        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "User not found or email already verified")
        })
        public ResponseEntity<ApiResponse<Void>> resendOtp(
                        @Parameter(description = "User email address", example = "john.doe@example.com") @RequestParam @Email String email) {
                authService.resendOtp(email);
                return ResponseEntity.ok(ApiResponse.ok(null, "OTP sent successfully"));
        }

        @PostMapping("/logout")
        @Operation(summary = "User logout", description = "Logout user and invalidate refresh token", security = @SecurityRequirement(name = "Bearer Authentication"))
        @ApiResponses(value = {
                        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Logout successful"),
                        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized")
        })
        public ResponseEntity<ApiResponse<Void>> logout(@AuthenticationPrincipal UserDetailsImpl userDetails) {
                authService.logout(userDetails.getUserId());

                // Clear the refresh token cookie at client side
                ResponseCookie clearCookie = ResponseCookie.from("refreshToken", "")
                                .httpOnly(true)
                                .secure(cookieSecure)
                                .path("/")
                                .maxAge(0)
                                .sameSite(cookieSameSite)
                                .build();

                return ResponseEntity.ok()
                                .header(HttpHeaders.SET_COOKIE, clearCookie.toString())
                                .body(ApiResponse.ok(null, "Logout successful"));
        }
}
