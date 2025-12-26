package com.taitrinh.online_auction.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailService {

    private final JavaMailSender mailSender;

    @Value("${spring.mail.username}")
    private String fromEmail;

    @Value("${app.name:BidStorm}")
    private String appName;

    @Value("${app.url:http://localhost:3000}")
    private String appUrl;

    /**
     * Send email verification OTP asynchronously
     */
    @Async
    public void sendEmailVerificationOTP(String toEmail, String otpCode, Integer expirationMinutes) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(fromEmail);
            helper.setTo(toEmail);
            helper.setSubject(String.format("%s - Xác thực Email", appName));
            helper.setText(buildEmailVerificationContent(otpCode, expirationMinutes), true);

            mailSender.send(message);
            log.info("OTP email sent successfully to: {}", toEmail);

        } catch (MessagingException | MailException e) {
            log.error("Failed to send OTP email to {}: {}", toEmail, e.getMessage());
            // In production, you might want to throw a custom exception
            // or implement retry logic
        }
    }

    /**
     * Build HTML email content for email verification OTP
     */
    private String buildEmailVerificationContent(String otpCode, Integer expirationMinutes) {
        return String.format(
                """
                        <!DOCTYPE html>
                        <html lang="vi">
                        <head>
                            <meta charset="UTF-8">
                            <meta name="viewport" content="width=device-width, initial-scale=1.0">
                            <title>Xác thực email - BidStorm</title>
                            <style>
                                body {
                                    font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, Arial, sans-serif;
                                    line-height: 1.6;
                                    color: #333333;
                                    background-color: #f4f4f4;
                                    margin: 0;
                                    padding: 0;
                                }
                                .container {
                                    max-width: 600px;
                                    margin: 40px auto;
                                    background-color: #ffffff;
                                    border-radius: 12px;
                                    overflow: hidden;
                                    box-shadow: 0 4px 12px rgba(0,0,0,0.08);
                                }
                                .header {
                                    background-color: #000000;
                                    color: #ffffff;
                                    text-align: center;
                                    padding: 30px 20px;
                                }
                                .header h1 {
                                    font-size: 28px;
                                    font-weight: bold;
                                    margin: 0;
                                    letter-spacing: 1px;
                                }
                                .content {
                                    padding: 40px 30px;
                                    text-align: center;
                                }
                                .greeting {
                                    font-size: 18px;
                                    margin-bottom: 20px;
                                }
                                .message {
                                    font-size: 16px;
                                    margin-bottom: 30px;
                                    color: #555555;
                                }
                                .otp-code {
                                    background-color: #000000;
                                    color: #ffffff;
                                    font-size: 36px;
                                    font-weight: bold;
                                    letter-spacing: 10px;
                                    padding: 20px;
                                    border-radius: 8px;
                                    display: inline-block;
                                    margin: 30px 0;
                                }
                                .warning {
                                    background-color: #f8f8f8;
                                    border-left: 4px solid #000000;
                                    padding: 15px 20px;
                                    margin: 30px 0;
                                    text-align: left;
                                    font-size: 14px;
                                }
                                .footer {
                                    background-color: #f9f9f9;
                                    text-align: center;
                                    padding: 20px;
                                    font-size: 12px;
                                    color: #888888;
                                }
                                .footer a {
                                    color: #000000;
                                    text-decoration: none;
                                }
                            </style>
                        </head>
                        <body>
                            <div class="container">
                                <div class="header">
                                    <h1>BidStorm</h1>
                                </div>

                                <div class="content">
                                    <p class="greeting">Xin chào,</p>

                                    <p class="message">Cảm ơn bạn đã đăng ký tài khoản trên BidStorm! Để xác thực địa chỉ email của bạn, vui lòng sử dụng mã OTP sau:</p>

                                    <div class="otp-code">%s</div>

                                    <div class="warning">
                                        <strong>Lưu ý quan trọng:</strong> Mã OTP này sẽ hết hạn sau <strong>%d phút</strong>. Vui lòng không chia sẻ mã này với bất kỳ ai.
                                    </div>

                                    <p class="message">Nếu bạn không yêu cầu mã xác thực này, vui lòng bỏ qua email này hoặc liên hệ hỗ trợ.</p>
                                </div>

                                <div class="footer">
                                    <p>© 2025 BidStorm. All rights reserved.</p>
                                    <p>Email tự động, vui lòng không trả lời. Nếu cần hỗ trợ, liên hệ <a href="mailto:support@bidstorm.com">support@bidstorm.com</a></p>
                                </div>
                            </div>
                        </body>
                        </html>
                        """,
                otpCode, expirationMinutes);
    }

    /**
     * Send welcome email after successful registration
     */
    @Async
    public void sendWelcomeEmail(String toEmail, String fullName) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(fromEmail);
            helper.setTo(toEmail);
            helper.setSubject(String.format("Chào mừng đến với %s!", appName));
            helper.setText(buildWelcomeEmailContent(fullName), true);

            mailSender.send(message);
            log.info("Welcome email sent successfully to: {}", toEmail);

        } catch (MessagingException | MailException e) {
            log.error("Failed to send welcome email to {}: {}", toEmail, e.getMessage());
        }
    }

    /**
     * Build HTML email content for welcome message
     */
    private String buildWelcomeEmailContent(String fullName) {
        return String.format(
                """
                        <!DOCTYPE html>
                        <html lang="vi">
                        <head>
                            <meta charset="UTF-8">
                            <meta name="viewport" content="width=device-width, initial-scale=1.0">
                            <title>Chào mừng đến với BidStorm</title>
                            <style>
                                body {
                                    font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, Arial, sans-serif;
                                    line-height: 1.6;
                                    color: #333333;
                                    background-color: #f4f4f4;
                                    margin: 0;
                                    padding: 0;
                                }
                                .container {
                                    max-width: 600px;
                                    margin: 40px auto;
                                    background-color: #ffffff;
                                    border-radius: 12px;
                                    overflow: hidden;
                                    box-shadow: 0 4px 12px rgba(0,0,0,0.08);
                                }
                                .header {
                                    background-color: #000000;
                                    color: #ffffff;
                                    text-align: center;
                                    padding: 40px 20px;
                                }
                                .header h1 {
                                    font-size: 32px;
                                    font-weight: bold;
                                    margin: 0;
                                    letter-spacing: 1px;
                                }
                                .header p {
                                    font-size: 18px;
                                    margin: 10px 0 0;
                                    opacity: 0.9;
                                }
                                .content {
                                    padding: 40px 30px;
                                    text-align: center;
                                }
                                .greeting {
                                    font-size: 20px;
                                    font-weight: 600;
                                    margin-bottom: 20px;
                                }
                                .message {
                                    font-size: 16px;
                                    color: #555555;
                                    margin-bottom: 20px;
                                }
                                .highlight {
                                    font-size: 18px;
                                    font-weight: 500;
                                    color: #000000;
                                    margin: 30px 0;
                                }
                                .footer {
                                    background-color: #f9f9f9;
                                    text-align: center;
                                    padding: 25px;
                                    font-size: 12px;
                                    color: #888888;
                                }
                                .footer a {
                                    color: #000000;
                                    text-decoration: none;
                                }
                            </style>
                        </head>
                        <body>
                            <div class="container">
                                <div class="header">
                                    <h1>Chào mừng đến với %s! 🎉</h1>
                                    <p>Bạn đã sẵn sàng khám phá những sản phẩm độc đáo</p>
                                </div>

                                <div class="content">
                                    <p class="greeting">Xin chào %s,</p>

                                    <p class="message">Email của bạn đã được xác thực thành công.</p>

                                    <p class="highlight">Bây giờ bạn có thể tham gia đấu giá và sở hữu những món hàng cao cấp, độc quyền ngay hôm nay!</p>

                                    <p class="message">Cảm ơn bạn đã gia nhập cộng đồng BidStorm.<br>Chúc bạn có những trải nghiệm thú vị và thành công trong các phiên đấu giá sắp tới.</p>
                                </div>

                                <div class="footer">
                                    <p>© 2025 %s. All rights reserved.</p>
                                    <p>Email tự động, vui lòng không trả lời. Nếu cần hỗ trợ, liên hệ <a href="mailto:support@bidstorm.com">support@bidstorm.com</a></p>
                                </div>
                            </div>
                        </body>
                        </html>
                        """,
                appName, fullName, appName);
    }

    /**
     * Send password reset OTP email asynchronously
     */
    @Async
    public void sendPasswordResetOtp(String toEmail, String otpCode, Integer expirationMinutes) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(fromEmail);
            helper.setTo(toEmail);
            helper.setSubject(String.format("%s - Đặt lại mật khẩu", appName));
            helper.setText(buildPasswordResetEmailContent(otpCode, expirationMinutes), true);

            mailSender.send(message);
            log.info("Password reset OTP email sent successfully to: {}", toEmail);

        } catch (MessagingException | MailException e) {
            log.error("Failed to send password reset OTP email to {}: {}", toEmail, e.getMessage());
        }
    }

    /**
     * Build HTML email content for password reset OTP
     */
    private String buildPasswordResetEmailContent(String otpCode, Integer expirationMinutes) {
        return String.format(
                """
                        <!DOCTYPE html>
                        <html lang="vi">
                        <head>
                            <meta charset="UTF-8">
                            <meta name="viewport" content="width=device-width, initial-scale=1.0">
                            <title>Đặt lại mật khẩu - BidStorm</title>
                            <style>
                                body {
                                    font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, Arial, sans-serif;
                                    line-height: 1.6;
                                    color: #333333;
                                    background-color: #f4f4f4;
                                    margin: 0;
                                    padding: 0;
                                }
                                .container {
                                    max-width: 600px;
                                    margin: 40px auto;
                                    background-color: #ffffff;
                                    border-radius: 12px;
                                    overflow: hidden;
                                    box-shadow: 0 4px 12px rgba(0,0,0,0.08);
                                }
                                .header {
                                    background-color: #000000;
                                    color: #ffffff;
                                    text-align: center;
                                    padding: 30px 20px;
                                }
                                .header h1 {
                                    font-size: 28px;
                                    font-weight: bold;
                                    margin: 0;
                                    letter-spacing: 1px;
                                }
                                .content {
                                    padding: 40px 30px;
                                    text-align: center;
                                }
                                .greeting {
                                    font-size: 18px;
                                    margin-bottom: 20px;
                                }
                                .message {
                                    font-size: 16px;
                                    margin-bottom: 30px;
                                    color: #555555;
                                }
                                .otp-code {
                                    background-color: #000000;
                                    color: #ffffff;
                                    font-size: 36px;
                                    font-weight: bold;
                                    letter-spacing: 10px;
                                    padding: 20px;
                                    border-radius: 8px;
                                    display: inline-block;
                                    margin: 30px 0;
                                }
                                .warning {
                                    background-color: #fff3cd;
                                    border-left: 4px solid #ffc107;
                                    padding: 15px 20px;
                                    margin: 30px 0;
                                    text-align: left;
                                    font-size: 14px;
                                }
                                .security-note {
                                    background-color: #f8f8f8;
                                    border-left: 4px solid #dc3545;
                                    padding: 15px 20px;
                                    margin: 30px 0;
                                    text-align: left;
                                    font-size: 14px;
                                }
                                .footer {
                                    background-color: #f9f9f9;
                                    text-align: center;
                                    padding: 20px;
                                    font-size: 12px;
                                    color: #888888;
                                }
                                .footer a {
                                    color: #000000;
                                    text-decoration: none;
                                }
                            </style>
                        </head>
                        <body>
                            <div class="container">
                                <div class="header">
                                    <h1>BidStorm</h1>
                                </div>

                                <div class="content">
                                    <p class="greeting">Xin chào,</p>

                                    <p class="message">Chúng tôi nhận được yêu cầu đặt lại mật khẩu cho tài khoản của bạn. Vui lòng sử dụng mã OTP sau để đặt lại mật khẩu:</p>

                                    <div class="otp-code">%s</div>

                                    <div class="warning">
                                        <strong>Lưu ý:</strong> Mã OTP này sẽ hết hạn sau <strong>%d phút</strong>.
                                    </div>

                                    <div class="security-note">
                                        <strong>Bảo mật:</strong> Nếu bạn KHÔNG yêu cầu đặt lại mật khẩu, vui lòng bỏ qua email này và xem xét thay đổi mật khẩu của bạn ngay lập tức để bảo vệ tài khoản.
                                    </div>
                                </div>

                                <div class="footer">
                                    <p>© 2025 BidStorm. All rights reserved.</p>
                                    <p>Email tự động, vui lòng không trả lời. Nếu cần hỗ trợ, liên hệ <a href="mailto:support@bidstorm.com">support@bidstorm.com</a></p>
                                </div>
                            </div>
                        </body>
                        </html>
                        """,
                otpCode, expirationMinutes);
    }

    /**
     * Send bid confirmation email to bidder
     */
    @Async
    public void sendBidConfirmationToBidder(String toEmail, String bidderName, String productTitle,
            java.math.BigDecimal bidAmount, boolean isWinning, String productSlug) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(fromEmail);
            helper.setTo(toEmail);
            helper.setSubject(String.format("%s - Xác nhận đấu giá sản phẩm: %s", appName, productTitle));
            helper.setText(
                    buildBidConfirmationEmailContent(bidderName, productTitle, bidAmount, isWinning, productSlug),
                    true);

            mailSender.send(message);
            log.info("Bid confirmation email sent to: {}", toEmail);
        } catch (MessagingException | MailException e) {
            log.error("Failed to send bid confirmation email to {}: {}", toEmail, e.getMessage());
        }
    }

    private String buildBidConfirmationEmailContent(String bidderName, String productTitle,
            java.math.BigDecimal bidAmount, boolean isWinning, String productSlug) {
        String productUrl = String.format("%s/san-pham/%s", appUrl, productSlug);
        String statusMessage = isWinning
                ? "Bạn hiện đang là người đặt giá cao nhất! 🎉"
                : "Lượt đấu giá của bạn đã được ghi nhận thành công.";
        String statusClass = isWinning ? "success-box" : "info-box";

        return String.format(
                """
                        <!DOCTYPE html>
                        <html lang="vi">
                        <head>
                            <meta charset="UTF-8">
                            <meta name="viewport" content="width=device-width, initial-scale=1.0">
                            <title>Xác nhận đấu giá - BidStorm</title>
                            <style>
                                body {
                                    font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, Arial, sans-serif;
                                    line-height: 1.6;
                                    color: #333333;
                                    background-color: #f4f4f4;
                                    margin: 0;
                                    padding: 0;
                                }
                                .container {
                                    max-width: 600px;
                                    margin: 40px auto;
                                    background-color: #ffffff;
                                    border-radius: 12px;
                                    overflow: hidden;
                                    box-shadow: 0 4px 12px rgba(0,0,0,0.08);
                                }
                                .header {
                                    background-color: #000000;
                                    color: #ffffff;
                                    text-align: center;
                                    padding: 30px 20px;
                                }
                                .header h1 {
                                    font-size: 28px;
                                    font-weight: bold;
                                    margin: 0;
                                    letter-spacing: 1px;
                                }
                                .content {
                                    padding: 40px 30px;
                                }
                                .greeting {
                                    font-size: 18px;
                                    margin-bottom: 20px;
                                }
                                .message {
                                    font-size: 16px;
                                    margin-bottom: 20px;
                                    color: #555555;
                                }
                                .success-box {
                                    background-color: #e8f5e9;
                                    border-left: 4px solid #4caf50;
                                    padding: 15px 20px;
                                    margin: 20px 0;
                                    font-size: 16px;
                                }
                                .info-box {
                                    background-color: #e3f2fd;
                                    border-left: 4px solid #2196f3;
                                    padding: 15px 20px;
                                    margin: 20px 0;
                                    font-size: 16px;
                                }
                                .bid-details {
                                    background-color: #f8f8f8;
                                    padding: 20px;
                                    border-radius: 8px;
                                    margin: 20px 0;
                                }
                                .bid-details p {
                                    margin: 10px 0;
                                    font-size: 15px;
                                }
                                .bid-details strong {
                                    color: #000000;
                                }
                                .footer {
                                    background-color: #f9f9f9;
                                    text-align: center;
                                    padding: 20px;
                                    font-size: 12px;
                                    color: #888888;
                                }
                                .footer a {
                                    color: #000000;
                                    text-decoration: none;
                                }
                            </style>
                        </head>
                        <body>
                            <div class="container">
                                <div class="header">
                                    <h1>BidStorm</h1>
                                </div>

                                <div class="content">
                                    <p class="greeting">Xin chào %s,</p>

                                    <div class="%s">
                                        <strong>%s</strong>
                                    </div>

                                    <div class="bid-details">
                                        <p><strong>Sản phẩm:</strong> %s</p>
                                        <p><strong>Giá đấu tối đa của bạn:</strong> %,d VND</p>
                                    </div>

                                    <p class="message">Chúng tôi sẽ thông báo cho bạn nếu có người đặt giá cao hơn.</p>

                                    <p style="text-align: center; margin: 30px 0;">
                                        <a href="%s" style="display: inline-block; padding: 15px 30px; background-color: #000000; color: #ffffff; text-decoration: none; border-radius: 8px; font-weight: bold; font-size: 16px;">Đi tới sản phẩm</a>
                                    </p>
                                </div>

                                <div class="footer">
                                    <p>© 2025 BidStorm. All rights reserved.</p>
                                    <p>Email tự động, vui lòng không trả lời. Nếu cần hỗ trợ, liên hệ <a href="mailto:support@bidstorm.com">support@bidstorm.com</a></p>
                                </div>
                            </div>
                        </body>
                        </html>
                        """,
                bidderName, statusClass, statusMessage, productTitle, bidAmount.longValue(), productUrl);
    }

    /**
     * Send notification to seller about new bid
     */
    @Async
    public void sendNewBidNotificationToSeller(String toEmail, String sellerName, String productTitle,
            String bidderName, java.math.BigDecimal newPrice, String productSlug) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(fromEmail);
            helper.setTo(toEmail);
            helper.setSubject(String.format("%s - Lượt đấu giá mới cho sản phẩm: %s", appName, productTitle));
            helper.setText(buildNewBidToSellerEmailContent(sellerName, productTitle, bidderName, newPrice, productSlug),
                    true);

            mailSender.send(message);
            log.info("New bid notification sent to seller: {}", toEmail);
        } catch (MessagingException | MailException e) {
            log.error("Failed to send new bid notification to seller {}: {}", toEmail, e.getMessage());
        }
    }

    private String buildNewBidToSellerEmailContent(String sellerName, String productTitle,
            String bidderName, java.math.BigDecimal newPrice, String productSlug) {
        String productUrl = String.format("%s/san-pham/%s", appUrl, productSlug);
        return String.format(
                """
                        <!DOCTYPE html>
                        <html lang="vi">
                        <head>
                            <meta charset="UTF-8">
                            <meta name="viewport" content="width=device-width, initial-scale=1.0">
                            <title>Lượt đấu giá mới - BidStorm</title>
                            <style>
                                body {
                                    font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, Arial, sans-serif;
                                    line-height: 1.6;
                                    color: #333333;
                                    background-color: #f4f4f4;
                                    margin: 0;
                                    padding: 0;
                                }
                                .container {
                                    max-width: 600px;
                                    margin: 40px auto;
                                    background-color: #ffffff;
                                    border-radius: 12px;
                                    overflow: hidden;
                                    box-shadow: 0 4px 12px rgba(0,0,0,0.08);
                                }
                                .header {
                                    background-color: #000000;
                                    color: #ffffff;
                                    text-align: center;
                                    padding: 30px 20px;
                                }
                                .header h1 {
                                    font-size: 28px;
                                    font-weight: bold;
                                    margin: 0;
                                    letter-spacing: 1px;
                                }
                                .content {
                                    padding: 40px 30px;
                                }
                                .greeting {
                                    font-size: 18px;
                                    margin-bottom: 20px;
                                }
                                .message {
                                    font-size: 16px;
                                    margin-bottom: 20px;
                                    color: #555555;
                                }
                                .success-box {
                                    background-color: #e8f5e9;
                                    border-left: 4px solid #4caf50;
                                    padding: 15px 20px;
                                    margin: 20px 0;
                                    font-size: 16px;
                                }
                                .bid-details {
                                    background-color: #f8f8f8;
                                    padding: 20px;
                                    border-radius: 8px;
                                    margin: 20px 0;
                                }
                                .bid-details p {
                                    margin: 10px 0;
                                    font-size: 15px;
                                }
                                .bid-details strong {
                                    color: #000000;
                                }
                                .footer {
                                    background-color: #f9f9f9;
                                    text-align: center;
                                    padding: 20px;
                                    font-size: 12px;
                                    color: #888888;
                                }
                                .footer a {
                                    color: #000000;
                                    text-decoration: none;
                                }
                            </style>
                        </head>
                        <body>
                            <div class="container">
                                <div class="header">
                                    <h1>BidStorm</h1>
                                </div>

                                <div class="content">
                                    <p class="greeting">Xin chào %s,</p>

                                    <div class="success-box">
                                        <strong>🎉 Tin tốt! Có người vừa đặt giá cho sản phẩm của bạn.</strong>
                                    </div>

                                    <div class="bid-details">
                                        <p><strong>Sản phẩm:</strong> %s</p>
                                        <p><strong>Người đặt giá:</strong> %s</p>
                                        <p><strong>Giá hiện tại:</strong> %,d VND</p>
                                    </div>

                                    <p class="message">Bạn có thể xem chi tiết lịch sử đấu giá trên trang sản phẩm.</p>

                                    <p style="text-align: center; margin: 30px 0;">
                                        <a href="%s" style="display: inline-block; padding: 15px 30px; background-color: #000000; color: #ffffff; text-decoration: none; border-radius: 8px; font-weight: bold; font-size: 16px;">Đi tới sản phẩm</a>
                                    </p>
                                </div>

                                <div class="footer">
                                    <p>© 2025 BidStorm. All rights reserved.</p>
                                    <p>Email tự động, vui lòng không trả lời. Nếu cần hỗ trợ, liên hệ <a href="mailto:support@bidstorm.com">support@bidstorm.com</a></p>
                                </div>
                            </div>
                        </body>
                        </html>
                        """,
                sellerName, productTitle, bidderName, newPrice.longValue(), productUrl);
    }

    /**
     * Send outbid notification to previous highest bidder
     */
    @Async
    public void sendOutbidNotification(String toEmail, String bidderName, String productTitle,
            java.math.BigDecimal newPrice, String productSlug) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(fromEmail);
            helper.setTo(toEmail);
            helper.setSubject(String.format("%s - Bạn đã bị trả giá cao hơn ở sản phẩm: %s", appName, productTitle));
            helper.setText(buildOutbidEmailContent(bidderName, productTitle, newPrice, productSlug), true);

            mailSender.send(message);
            log.info("Outbid notification sent to: {}", toEmail);
        } catch (MessagingException | MailException e) {
            log.error("Failed to send outbid notification to {}: {}", toEmail, e.getMessage());
        }
    }

    private String buildOutbidEmailContent(String bidderName, String productTitle,
            java.math.BigDecimal newPrice, String productSlug) {
        String productUrl = String.format("%s/san-pham/%s", appUrl, productSlug);
        return String.format(
                """
                        <!DOCTYPE html>
                        <html lang="vi">
                        <head>
                            <meta charset="UTF-8">
                            <meta name="viewport" content="width=device-width, initial-scale=1.0">
                            <title>Bạn đã bị vượt giá - BidStorm</title>
                            <style>
                                body {
                                    font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, Arial, sans-serif;
                                    line-height: 1.6;
                                    color: #333333;
                                    background-color: #f4f4f4;
                                    margin: 0;
                                    padding: 0;
                                }
                                .container {
                                    max-width: 600px;
                                    margin: 40px auto;
                                    background-color: #ffffff;
                                    border-radius: 12px;
                                    overflow: hidden;
                                    box-shadow: 0 4px 12px rgba(0,0,0,0.08);
                                }
                                .header {
                                    background-color: #000000;
                                    color: #ffffff;
                                    text-align: center;
                                    padding: 30px 20px;
                                }
                                .header h1 {
                                    font-size: 28px;
                                    font-weight: bold;
                                    margin: 0;
                                    letter-spacing: 1px;
                                }
                                .content {
                                    padding: 40px 30px;
                                }
                                .greeting {
                                    font-size: 18px;
                                    margin-bottom: 20px;
                                }
                                .message {
                                    font-size: 16px;
                                    margin-bottom: 20px;
                                    color: #555555;
                                }
                                .warning-box {
                                    background-color: #fff3cd;
                                    border-left: 4px solid #ffc107;
                                    padding: 15px 20px;
                                    margin: 20px 0;
                                    font-size: 16px;
                                }
                                .bid-details {
                                    background-color: #f8f8f8;
                                    padding: 20px;
                                    border-radius: 8px;
                                    margin: 20px 0;
                                }
                                .bid-details p {
                                    margin: 10px 0;
                                    font-size: 15px;
                                }
                                .bid-details strong {
                                    color: #000000;
                                }
                                .footer {
                                    background-color: #f9f9f9;
                                    text-align: center;
                                    padding: 20px;
                                    font-size: 12px;
                                    color: #888888;
                                }
                                .footer a {
                                    color: #000000;
                                    text-decoration: none;
                                }
                            </style>
                        </head>
                        <body>
                            <div class="container">
                                <div class="header">
                                    <h1>BidStorm</h1>
                                </div>

                                <div class="content">
                                    <p class="greeting">Xin chào %s,</p>

                                    <div class="warning-box">
                                        <strong>⚠️ Có người đã đặt giá cao hơn bạn!</strong>
                                    </div>

                                    <div class="bid-details">
                                        <p><strong>Sản phẩm:</strong> %s</p>
                                        <p><strong>Giá hiện tại:</strong> %,d VND</p>
                                    </div>

                                    <p class="message">Đặt giá mới ngay để tiếp tục tham gia đấu giá!</p>

                                <p style="text-align: center; margin: 30px 0;">
                                    <a href="%s" style="display: inline-block; padding: 15px 30px; background-color: #000000; color: #ffffff; text-decoration: none; border-radius: 8px; font-weight: bold; font-size: 16px;">Đi tới sản phẩm</a>
                                </p>
                            </div>

                            <div class="footer">
                                    <p>© 2025 BidStorm. All rights reserved.</p>
                                    <p>Email tự động, vui lòng không trả lời. Nếu cần hỗ trợ, liên hệ <a href="mailto:support@bidstorm.com">support@bidstorm.com</a></p>
                                </div>
                            </div>
                        </body>
                        </html>
                        """,
                bidderName, productTitle, newPrice.longValue(), productUrl);
    }

    /**
     * Send bid rejection notification to bidder
     */
    @Async
    public void sendBidRejectionEmail(String toEmail, String bidderName, String productTitle, String productSlug) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(fromEmail);
            helper.setTo(toEmail);
            helper.setSubject(String.format("%s - Đấu giá bị từ chối cho sản phẩm: %s", appName, productTitle));
            helper.setText(buildBidRejectionEmailContent(bidderName, productTitle, productSlug), true);

            mailSender.send(message);
            log.info("Bid rejection email sent to: {}", toEmail);
        } catch (MessagingException | MailException e) {
            log.error("Failed to send bid rejection email to {}: {}", toEmail, e.getMessage());
        }
    }

    private String buildBidRejectionEmailContent(String bidderName, String productTitle, String productSlug) {
        String productUrl = String.format("%s/san-pham/%s", appUrl, productSlug);
        return String.format(
                """
                        <!DOCTYPE html>
                        <html lang="vi">
                        <head>
                            <meta charset="UTF-8">
                            <meta name="viewport" content="width=device-width, initial-scale=1.0">
                            <title>Lượt đấu giá bị từ chối - BidStorm</title>
                            <style>
                                body {
                                    font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, Arial, sans-serif;
                                    line-height: 1.6;
                                    color: #333333;
                                    background-color: #f4f4f4;
                                    margin: 0;
                                    padding: 0;
                                }
                                .container {
                                    max-width: 600px;
                                    margin: 40px auto;
                                    background-color: #ffffff;
                                    border-radius: 12px;
                                    overflow: hidden;
                                    box-shadow: 0 4px 12px rgba(0,0,0,0.08);
                                }
                                .header {
                                    background-color: #000000;
                                    color: #ffffff;
                                    text-align: center;
                                    padding: 30px 20px;
                                }
                                .header h1 {
                                    font-size: 28px;
                                    font-weight: bold;
                                    margin: 0;
                                    letter-spacing: 1px;
                                }
                                .content {
                                    padding: 40px 30px;
                                }
                                .greeting {
                                    font-size: 18px;
                                    margin-bottom: 20px;
                                }
                                .message {
                                    font-size: 16px;
                                    margin-bottom: 20px;
                                    color: #555555;
                                }
                                .error-box {
                                    background-color: #f8d7da;
                                    border-left: 4px solid #dc3545;
                                    padding: 15px 20px;
                                    margin: 20px 0;
                                    font-size: 16px;
                                }
                                .product-info {
                                    background-color: #f8f8f8;
                                    padding: 20px;
                                    border-radius: 8px;
                                    margin: 20px 0;
                                }
                                .product-info p {
                                    margin: 10px 0;
                                    font-size: 15px;
                                }
                                .product-info strong {
                                    color: #000000;
                                }
                                .footer {
                                    background-color: #f9f9f9;
                                    text-align: center;
                                    padding: 20px;
                                    font-size: 12px;
                                    color: #888888;
                                }
                                .footer a {
                                    color: #000000;
                                    text-decoration: none;
                                }
                            </style>
                        </head>
                        <body>
                            <div class="container">
                                <div class="header">
                                    <h1>BidStorm</h1>
                                </div>

                                <div class="content">
                                    <p class="greeting">Xin chào %s,</p>

                                    <div class="error-box">
                                        <strong>❌ Người bán đã từ chối lượt đấu giá của bạn</strong>
                                    </div>

                                    <div class="product-info">
                                        <p><strong>Sản phẩm:</strong> %s</p>
                                    </div>

                                    <p class="message">Bạn không còn được phép tham gia đấu giá sản phẩm này.</p>
                                    <p class="message">Nếu có thắc mắc, vui lòng liên hệ với người bán hoặc bộ phận hỗ trợ.</p>
                                </div>

                                <div class="footer">
                                    <p>© 2025 BidStorm. All rights reserved.</p>
                                    <p>Email tự động, vui lòng không trả lời. Nếu cần hỗ trợ, liên hệ <a href="mailto:support@bidstorm.com">support@bidstorm.com</a></p>
                                </div>
                            </div>
                        </body>
                        </html>
                        """,
                bidderName, productTitle, productUrl);
    }

    /**
     * Send notification to seller when buyer asks a question
     */
    @Async
    public void sendNewQuestionToSeller(String toEmail, String sellerName, String productTitle,
            String askerName, String questionText, String productSlug, Long commentId) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(fromEmail);
            helper.setTo(toEmail);
            helper.setSubject(String.format("%s - Câu hỏi mới về sản phẩm: %s", appName, productTitle));
            helper.setText(
                    buildNewQuestionEmailContent(sellerName, productTitle, askerName, questionText, productSlug,
                            commentId),
                    true);

            mailSender.send(message);
            log.info("New question notification sent to seller: {}", toEmail);
        } catch (MessagingException | MailException e) {
            log.error("Failed to send new question notification to seller {}: {}", toEmail, e.getMessage());
        }
    }

    private String buildNewQuestionEmailContent(String sellerName, String productTitle,
            String askerName, String questionText, String productSlug, Long commentId) {
        String productUrl = String.format("%s/san-pham/%s?comment_id=%d", appUrl, productSlug, commentId);
        String truncatedQuestion = questionText.length() > 200
                ? questionText.substring(0, 200) + "..."
                : questionText;

        return String.format(
                """
                        <!DOCTYPE html>
                        <html lang="vi">
                        <head>
                            <meta charset="UTF-8">
                            <meta name="viewport" content="width=device-width, initial-scale=1.0">
                            <title>Câu hỏi mới - BidStorm</title>
                            <style>
                                body {
                                    font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, Arial, sans-serif;
                                    line-height: 1.6;
                                    color: #333333;
                                    background-color: #f4f4f4;
                                    margin: 0;
                                    padding: 0;
                                }
                                .container {
                                    max-width: 600px;
                                    margin: 40px auto;
                                    background-color: #ffffff;
                                    border-radius: 12px;
                                    overflow: hidden;
                                    box-shadow: 0 4px 12px rgba(0,0,0,0.08);
                                }
                                .header {
                                    background-color: #000000;
                                    color: #ffffff;
                                    text-align: center;
                                    padding: 30px 20px;
                                }
                                .header h1 {
                                    font-size: 28px;
                                    font-weight: bold;
                                    margin: 0;
                                    letter-spacing: 1px;
                                }
                                .content {
                                    padding: 40px 30px;
                                }
                                .greeting {
                                    font-size: 18px;
                                    margin-bottom: 20px;
                                }
                                .message {
                                    font-size: 16px;
                                    margin-bottom: 20px;
                                    color: #555555;
                                }
                                .info-box {
                                    background-color: #e3f2fd;
                                    border-left: 4px solid #2196f3;
                                    padding: 15px 20px;
                                    margin: 20px 0;
                                    font-size: 16px;
                                }
                                .question-box {
                                    background-color: #f8f8f8;
                                    padding: 20px;
                                    border-radius: 8px;
                                    margin: 20px 0;
                                    border-left: 3px solid #2196f3;
                                }
                                .question-box p {
                                    margin: 10px 0;
                                    font-size: 15px;
                                }
                                .question-box strong {
                                    color: #000000;
                                }
                                .footer {
                                    background-color: #f9f9f9;
                                    text-align: center;
                                    padding: 20px;
                                    font-size: 12px;
                                    color: #888888;
                                }
                                .footer a {
                                    color: #000000;
                                    text-decoration: none;
                                }
                            </style>
                        </head>
                        <body>
                            <div class="container">
                                <div class="header">
                                    <h1>BidStorm</h1>
                                </div>

                                <div class="content">
                                    <p class="greeting">Xin chào %s,</p>

                                    <div class="info-box">
                                        <strong>💬 Bạn có câu hỏi mới về sản phẩm!</strong>
                                    </div>

                                    <div class="question-box">
                                        <p><strong>Sản phẩm:</strong> %s</p>
                                        <p><strong>Người hỏi:</strong> %s</p>
                                        <p><strong>Câu hỏi:</strong></p>
                                        <p style="font-style: italic; color: #555;">"%s"</p>
                                    </div>

                                    <p class="message">Vui lòng trả lời câu hỏi để giúp người mua hiểu rõ hơn về sản phẩm.</p>

                                    <p style="text-align: center; margin: 30px 0;">
                                        <a href="%s" style="display: inline-block; padding: 15px 30px; background-color: #000000; color: #ffffff; text-decoration: none; border-radius: 8px; font-weight: bold; font-size: 16px;">Trả lời ngay</a>
                                    </p>
                                </div>

                                <div class="footer">
                                    <p>© 2025 BidStorm. All rights reserved.</p>
                                    <p>Email tự động, vui lòng không trả lời. Nếu cần hỗ trợ, liên hệ <a href="mailto:support@bidstorm.com">support@bidstorm.com</a></p>
                                </div>
                            </div>
                        </body>
                        </html>
                        """,
                sellerName, productTitle, askerName, truncatedQuestion, productUrl);
    }

    /**
     * Send notification to participants when seller replies to Q&A
     */
    @Async
    public void sendSellerReplyNotification(String toEmail, String participantName, String productTitle,
            String replyText, String productSlug, Long commentId) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(fromEmail);
            helper.setTo(toEmail);
            helper.setSubject(String.format("%s - Người bán đã trả lời về sản phẩm: %s", appName, productTitle));
            helper.setText(
                    buildSellerReplyEmailContent(participantName, productTitle, replyText, productSlug, commentId),
                    true);

            mailSender.send(message);
            log.info("Seller reply notification sent to: {}", toEmail);
        } catch (MessagingException | MailException e) {
            log.error("Failed to send seller reply notification to {}: {}", toEmail, e.getMessage());
        }
    }

    private String buildSellerReplyEmailContent(String participantName, String productTitle,
            String replyText, String productSlug, Long commentId) {
        String productUrl = String.format("%s/san-pham/%s?comment_id=%d", appUrl, productSlug, commentId);
        String truncatedReply = replyText.length() > 200
                ? replyText.substring(0, 200) + "..."
                : replyText;

        return String.format(
                """
                        <!DOCTYPE html>
                        <html lang="vi">
                        <head>
                            <meta charset="UTF-8">
                            <meta name="viewport" content="width=device-width, initial-scale=1.0">
                            <title>Người bán đã trả lời - BidStorm</title>
                            <style>
                                body {
                                    font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, Arial, sans-serif;
                                    line-height: 1.6;
                                    color: #333333;
                                    background-color: #f4f4f4;
                                    margin: 0;
                                    padding: 0;
                                }
                                .container {
                                    max-width: 600px;
                                    margin: 40px auto;
                                    background-color: #ffffff;
                                    border-radius: 12px;
                                    overflow: hidden;
                                    box-shadow: 0 4px 12px rgba(0,0,0,0.08);
                                }
                                .header {
                                    background-color: #000000;
                                    color: #ffffff;
                                    text-align: center;
                                    padding: 30px 20px;
                                }
                                .header h1 {
                                    font-size: 28px;
                                    font-weight: bold;
                                    margin: 0;
                                    letter-spacing: 1px;
                                }
                                .content {
                                    padding: 40px 30px;
                                }
                                .greeting {
                                    font-size: 18px;
                                    margin-bottom: 20px;
                                }
                                .message {
                                    font-size: 16px;
                                    margin-bottom: 20px;
                                    color: #555555;
                                }
                                .success-box {
                                    background-color: #e8f5e9;
                                    border-left: 4px solid #4caf50;
                                    padding: 15px 20px;
                                    margin: 20px 0;
                                    font-size: 16px;
                                }
                                .reply-box {
                                    background-color: #f8f8f8;
                                    padding: 20px;
                                    border-radius: 8px;
                                    margin: 20px 0;
                                    border-left: 3px solid #4caf50;
                                }
                                .reply-box p {
                                    margin: 10px 0;
                                    font-size: 15px;
                                }
                                .reply-box strong {
                                    color: #000000;
                                }
                                .footer {
                                    background-color: #f9f9f9;
                                    text-align: center;
                                    padding: 20px;
                                    font-size: 12px;
                                    color: #888888;
                                }
                                .footer a {
                                    color: #000000;
                                    text-decoration: none;
                                }
                            </style>
                        </head>
                        <body>
                            <div class="container">
                                <div class="header">
                                    <h1>BidStorm</h1>
                                </div>

                                <div class="content">
                                    <p class="greeting">Xin chào %s,</p>

                                    <div class="success-box">
                                        <strong>✅ Người bán đã trả lời câu hỏi của bạn!</strong>
                                    </div>

                                    <div class="reply-box">
                                        <p><strong>Sản phẩm:</strong> %s</p>
                                        <p><strong>Câu trả lời:</strong></p>
                                        <p style="font-style: italic; color: #555;">"%s"</p>
                                    </div>

                                    <p class="message">Xem toàn bộ cuộc trò chuyện trên trang sản phẩm.</p>

                                    <p style="text-align: center; margin: 30px 0;">
                                        <a href="%s" style="display: inline-block; padding: 15px 30px; background-color: #000000; color: #ffffff; text-decoration: none; border-radius: 8px; font-weight: bold; font-size: 16px;">Đi tới sản phẩm</a>
                                    </p>
                                </div>

                                <div class="footer">
                                    <p>© 2025 BidStorm. All rights reserved.</p>
                                    <p>Email tự động, vui lòng không trả lời. Nếu cần hỗ trợ, liên hệ <a href="mailto:support@bidstorm.com">support@bidstorm.com</a></p>
                                </div>
                            </div>
                        </body>
                        </html>
                        """,
                participantName, productTitle, truncatedReply, productUrl);
    }

    /**
     * Send notification to participants about new activity on product they're
     * interested in
     */
    @Async
    public void sendProductActivityNotification(String toEmail, String participantName, String productTitle,
            String activityText, String productSlug, Long commentId) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(fromEmail);
            helper.setTo(toEmail);
            helper.setSubject(String.format("%s - Hoạt động mới về sản phẩm: %s", appName, productTitle));
            helper.setText(buildProductActivityEmailContent(participantName, productTitle, activityText, productSlug,
                    commentId), true);

            mailSender.send(message);
            log.info("Product activity notification sent to: {}", toEmail);
        } catch (MessagingException | MailException e) {
            log.error("Failed to send product activity notification to {}: {}", toEmail, e.getMessage());
        }
    }

    private String buildProductActivityEmailContent(String participantName, String productTitle,
            String activityText, String productSlug, Long commentId) {
        String productUrl = String.format("%s/san-pham/%s?comment_id=%d", appUrl, productSlug, commentId);
        String truncatedActivity = activityText.length() > 200
                ? activityText.substring(0, 200) + "..."
                : activityText;

        return String.format(
                """
                        <!DOCTYPE html>
                        <html lang="vi">
                        <head>
                            <meta charset="UTF-8">
                            <meta name="viewport" content="width=device-width, initial-scale=1.0">
                            <title>Hoạt động mới - BidStorm</title>
                            <style>
                                body {
                                    font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, Arial, sans-serif;
                                    line-height: 1.6;
                                    color: #333333;
                                    background-color: #f4f4f4;
                                    margin: 0;
                                    padding: 0;
                                }
                                .container {
                                    max-width: 600px;
                                    margin: 40px auto;
                                    background-color: #ffffff;
                                    border-radius: 12px;
                                    overflow: hidden;
                                    box-shadow: 0 4px 12px rgba(0,0,0,0.08);
                                }
                                .header {
                                    background-color: #000000;
                                    color: #ffffff;
                                    text-align: center;
                                    padding: 30px 20px;
                                }
                                .header h1 {
                                    font-size: 28px;
                                    font-weight: bold;
                                    margin: 0;
                                    letter-spacing: 1px;
                                }
                                .content {
                                    padding: 40px 30px;
                                }
                                .greeting {
                                    font-size: 18px;
                                    margin-bottom: 20px;
                                }
                                .message {
                                    font-size: 16px;
                                    margin-bottom: 20px;
                                    color: #555555;
                                }
                                .info-box {
                                    background-color: #e3f2fd;
                                    border-left: 4px solid #2196f3;
                                    padding: 15px 20px;
                                    margin: 20px 0;
                                    font-size: 16px;
                                }
                                .activity-box {
                                    background-color: #f8f8f8;
                                    padding: 20px;
                                    border-radius: 8px;
                                    margin: 20px 0;
                                    border-left: 3px solid #2196f3;
                                }
                                .activity-box p {
                                    margin: 10px 0;
                                    font-size: 15px;
                                }
                                .activity-box strong {
                                    color: #000000;
                                }
                                .footer {
                                    background-color: #f9f9f9;
                                    text-align: center;
                                    padding: 20px;
                                    font-size: 12px;
                                    color: #888888;
                                }
                                .footer a {
                                    color: #000000;
                                    text-decoration: none;
                                }
                            </style>
                        </head>
                        <body>
                            <div class="container">
                                <div class="header">
                                    <h1>BidStorm</h1>
                                </div>

                                <div class="content">
                                    <p class="greeting">Xin chào %s,</p>

                                    <div class="info-box">
                                        <strong>💬 Người bán đã trả lời một câu hỏi về sản phẩm bạn quan tâm!</strong>
                                    </div>

                                    <div class="activity-box">
                                        <p><strong>Sản phẩm:</strong> %s</p>
                                        <p><strong>Trả lời:</strong></p>
                                        <p style="font-style: italic; color: #555;">"%s"</p>
                                    </div>

                                    <p class="message">Xem toàn bộ cuộc trò chuyện để cập nhật thông tin mới nhất về sản phẩm.</p>

                                    <p style="text-align: center; margin: 30px 0;">
                                        <a href="%s" style="display: inline-block; padding: 15px 30px; background-color: #000000; color: #ffffff; text-decoration: none; border-radius: 8px; font-weight: bold; font-size: 16px;">Đi tới sản phẩm</a>
                                    </p>
                                </div>

                                <div class="footer">
                                    <p>© 2025 BidStorm. All rights reserved.</p>
                                    <p>Email tự động, vui lòng không trả lời. Nếu cần hỗ trợ, liên hệ <a href="mailto:support@bidstorm.com">support@bidstorm.com</a></p>
                                </div>
                            </div>
                        </body>
                        </html>
                        """,
                participantName, productTitle, truncatedActivity, productUrl);
    }
}
