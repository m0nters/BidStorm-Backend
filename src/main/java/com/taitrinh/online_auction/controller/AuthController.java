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
import com.taitrinh.online_auction.dto.auth.ForgotPasswordRequest;
import com.taitrinh.online_auction.dto.auth.LoginRequest;
import com.taitrinh.online_auction.dto.auth.LoginResponse;
import com.taitrinh.online_auction.dto.auth.OtpRequest;
import com.taitrinh.online_auction.dto.auth.RegisterRequest;
import com.taitrinh.online_auction.dto.auth.ResetPasswordRequest;
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
        @Operation(summary = "Register a new user", description = "Register a new bidder account combined with sending OTP to email for email verification. Password must be at least 8 characters with uppercase, lowercase, number, and special character.")
        @ApiResponses(value = {
                        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "User registered successfully. OTP sent to email for verification."),
                        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid input or email already exists")
        })
        public ResponseEntity<ApiResponse<Void>> register(@Valid @RequestBody RegisterRequest request) {
                authService.register(request);
                return ResponseEntity.status(HttpStatus.CREATED).body(
                                ApiResponse.created(null,
                                                "Đăng ký thành công. Vui lòng kiểm tra email để xác nhận OTP."));
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
                                .body(ApiResponse.ok(response, "Đăng nhập thành công"));
        }

        @PostMapping("/refresh")
        @Operation(summary = "Refresh access token", description = "Generate new access and refresh tokens using the refresh token from httpOnly cookie. Implements token rotation for enhanced security.")
        @ApiResponses(value = {
                        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Token refreshed successfully. New refresh token set in cookie."),
                        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Invalid or expired refresh token")
        })
        public ResponseEntity<ApiResponse<LoginResponse>> refreshToken(
                        @CookieValue(name = "refreshToken", required = true) String refreshToken) {
                LoginResponse response = authService.refreshToken(refreshToken);

                // TOKEN ROTATION: Set new refresh token in HttpOnly cookie
                ResponseCookie newRefreshTokenCookie = ResponseCookie.from("refreshToken", response.getRefreshToken())
                                .httpOnly(true)
                                .secure(cookieSecure)
                                .path("/")
                                .maxAge(refreshTokenExpiration / 1000) // Convert milliseconds to seconds
                                .sameSite(cookieSameSite)
                                .build();

                // Remove refresh token from response body (it's in the cookie)
                response.setRefreshToken(null);

                return ResponseEntity.ok()
                                .header(HttpHeaders.SET_COOKIE, newRefreshTokenCookie.toString())
                                .body(ApiResponse.ok(response, "Token đã được cập nhật"));
        }

        @PostMapping("/verify-email-otp")
        @Operation(summary = "Verify email with OTP", description = "Verify user email address using the OTP sent during registration")
        @ApiResponses(value = {
                        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Email verified successfully"),
                        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid or expired OTP")
        })
        public ResponseEntity<ApiResponse<Void>> verifyEmailOtp(@Valid @RequestBody OtpRequest request) {
                authService.verifyEmailOtp(request);
                return ResponseEntity.ok(ApiResponse.ok(null, "Email đã được xác nhận thành công"));
        }

        @PostMapping("/resend-email-verification-otp")
        @Operation(summary = "Resend OTP", description = "Resend verification OTP to user's email address")
        @ApiResponses(value = {
                        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "OTP sent successfully"),
                        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "User not found or email already verified")
        })
        public ResponseEntity<ApiResponse<Void>> resendEmailVerificationOtp(
                        @Parameter(description = "User email address", example = "john.doe@example.com") @RequestParam @Email String email) {
                authService.resendEmailVerificationOtp(email);
                return ResponseEntity.ok(ApiResponse.ok(null, "OTP đã được gửi lại thành công"));
        }

        @PostMapping("/forgot-password")
        @Operation(summary = "Request password reset", description = "Send OTP to email for password reset (for resend password reset request, use this endpoint too). Returns success even if email doesn't exist (security best practice).")
        @ApiResponses(value = {
                        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "If email exists, OTP has been sent"),
                        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid input")
        })
        public ResponseEntity<ApiResponse<Void>> forgotPassword(
                        @Valid @RequestBody ForgotPasswordRequest request) {
                authService.forgotPassword(request.getEmail());
                return ResponseEntity.ok(ApiResponse.ok(null,
                                "Yêu cầu quên mật khẩu đã được gửi lại thành công"));
        }

        @PostMapping("/verify-reset-password-otp")
        @Operation(summary = "Verify password reset OTP", description = "Verify the OTP. This allows the frontend to validate the OTP before proceeding to the password change page.")
        @ApiResponses(value = {
                        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "OTP is valid"),
                        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid or expired OTP")
        })
        public ResponseEntity<ApiResponse<Void>> verifyResetPasswordOtp(
                        @Valid @RequestBody OtpRequest request) {
                authService.verifyResetPasswordOtp(request.getEmail(), request.getOtpCode());
                return ResponseEntity.ok(
                                ApiResponse.ok(null, "OTP đã được xác nhận thành công. Bạn có thể thay đổi mật khẩu"));
        }

        @PostMapping("/reset-password")
        @Operation(summary = "Reset password with OTP", description = "Reset password after OTP verification. The OTP must have been verified in the previous step.")
        @ApiResponses(value = {
                        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Password reset successfully"),
                        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "User not found")
        })
        public ResponseEntity<ApiResponse<Void>> resetPassword(
                        @Valid @RequestBody ResetPasswordRequest request) {
                authService.resetPassword(request.getEmail(), request.getNewPassword());
                return ResponseEntity.ok(ApiResponse.ok(null,
                                "Mật khẩu đã được thay đổi thành công. Tất cả các phiên hoạt động đã được kết thúc. Vui lòng đăng nhập lại với mật khẩu mới."));
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
                                .body(ApiResponse.ok(null, "Đăng xuất thành công"));
        }

}
