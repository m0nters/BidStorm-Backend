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

    /**
     * Send email verification OTP asynchronously
     * 
     * @param toEmail           Recipient email address
     * @param otpCode           The OTP code to send
     * @param expirationMinutes How long the OTP is valid
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
            helper.setSubject(String.format("Welcome to %s!", appName));
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
}
