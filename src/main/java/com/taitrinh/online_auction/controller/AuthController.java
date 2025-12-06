package com.taitrinh.online_auction.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.taitrinh.online_auction.dto.ApiResponse;
import com.taitrinh.online_auction.dto.auth.LoginRequest;
import com.taitrinh.online_auction.dto.auth.LoginResponse;
import com.taitrinh.online_auction.dto.auth.RefreshTokenRequest;
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

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@Validated
@Tag(name = "Authentication", description = "User authentication and registration endpoints")
public class AuthController {

        private final AuthService authService;

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
        @Operation(summary = "User login", description = "Authenticate user and receive JWT access token and refresh token")
        @ApiResponses(value = {
                        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Login successful"),
                        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Invalid credentials or account inactive")
        })
        public ResponseEntity<ApiResponse<LoginResponse>> login(@Valid @RequestBody LoginRequest request) {
                LoginResponse response = authService.login(request);
                return ResponseEntity.ok(ApiResponse.ok(response, "Login successful"));
        }

        @PostMapping("/refresh")
        @Operation(summary = "Refresh access token", description = "Generate a new access token using a valid refresh token")
        @ApiResponses(value = {
                        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Token refreshed successfully"),
                        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Invalid or expired refresh token")
        })
        public ResponseEntity<ApiResponse<LoginResponse>> refreshToken(
                        @Valid @RequestBody RefreshTokenRequest request) {
                LoginResponse response = authService.refreshToken(request.getRefreshToken());
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
                return ResponseEntity.ok(ApiResponse.ok(null, "Logout successful"));
        }
}
