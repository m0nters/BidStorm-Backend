package com.taitrinh.online_auction.service.email;

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

/**
 * Email service for authentication-related notifications
 * Handles OTP verification, welcome emails, and password reset
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AuthEmailService {

    private final JavaMailSender mailSender;
    private final EmailTemplateService templateService;

    @Value("${spring.mail.username}")
    private String fromEmail;

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
            helper.setSubject(String.format("%s - Xác thực Email", templateService.getAppName()));
            helper.setText(buildEmailVerificationContent(otpCode, expirationMinutes), true);

            mailSender.send(message);
            log.info("OTP email sent successfully to: {}", toEmail);

        } catch (MessagingException | MailException e) {
            log.error("Failed to send OTP email to {}: {}", toEmail, e.getMessage());
        }
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
            helper.setSubject(String.format("Chào mừng đến với %s!", templateService.getAppName()));
            helper.setText(buildWelcomeEmailContent(fullName), true);

            mailSender.send(message);
            log.info("Welcome email sent successfully to: {}", toEmail);

        } catch (MessagingException | MailException e) {
            log.error("Failed to send welcome email to {}: {}", toEmail, e.getMessage());
        }
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
            helper.setSubject(String.format("%s - Đặt lại mật khẩu", templateService.getAppName()));
            helper.setText(buildPasswordResetEmailContent(otpCode, expirationMinutes), true);

            mailSender.send(message);
            log.info("Password reset OTP email sent successfully to: {}", toEmail);

        } catch (MessagingException | MailException e) {
            log.error("Failed to send password reset OTP email to {}: {}", toEmail, e.getMessage());
        }
    }

    // ========== Private Content Builders ==========

    private String buildEmailVerificationContent(String otpCode, Integer expirationMinutes) {
        String content = String.format(
                """
                        <p class="greeting">Xin chào,</p>

                        <p class="message">Cảm ơn bạn đã đăng ký tài khoản trên BidStorm! Để xác thực địa chỉ email của bạn, vui lòng sử dụng mã OTP sau:</p>

                        <div class="otp-code" style="text-align: center;">%s</div>

                        <div class="warning">
                            <strong>Lưu ý quan trọng:</strong> Mã OTP này sẽ hết hạn sau <strong>%d phút</strong>. Vui lòng không chia sẻ mã này với bất kỳ ai.
                        </div>

                        <p class="message">Nếu bạn không yêu cầu mã xác thực này, vui lòng bỏ qua email này hoặc liên hệ hỗ trợ.</p>
                        """,
                otpCode, expirationMinutes);

        return templateService.buildEmail("Xác thực email - BidStorm", content);
    }

    private String buildWelcomeEmailContent(String fullName) {
        String content = String.format(
                """
                        <div style="text-align: center;">
                            <h2 style="font-size: 24px; color: #000000; margin-bottom: 20px;">Chào mừng đến với %s! 🎉</h2>
                            <p style="font-size: 16px; color: #555555;">Bạn đã sẵn sàng khám phá những sản phẩm độc đáo</p>
                        </div>

                        <p class="greeting">Xin chào %s,</p>

                        <p class="message">Email của bạn đã được xác thực thành công.</p>

                        <p style="font-size: 18px; font-weight: 500; color: #000000; margin: 30px 0;">Bây giờ bạn có thể tham gia đấu giá và sở hữu những món hàng cao cấp, độc quyền ngay hôm nay!</p>

                        <p class="message">Cảm ơn bạn đã gia nhập cộng đồng BidStorm.<br>Chúc bạn có những trải nghiệm thú vị và thành công trong các phiên đấu giá sắp tới.</p>
                        """,
                templateService.getAppName(), fullName);

        return templateService.buildEmail("Chào mừng đến với BidStorm", content);
    }

    private String buildPasswordResetEmailContent(String otpCode, Integer expirationMinutes) {
        String content = String.format(
                """
                        <p class="greeting">Xin chào,</p>

                        <p class="message">Chúng tôi nhận được yêu cầu đặt lại mật khẩu cho tài khoản của bạn. Vui lòng sử dụng mã OTP sau để đặt lại mật khẩu:</p>

                        <div class="otp-code" style="text-align: center;">%s</div>

                        <div class="warning-box">
                            <strong>Lưu ý:</strong> Mã OTP này sẽ hết hạn sau <strong>%d phút</strong>.
                        </div>

                        <div class="danger-box">
                            <strong>Bảo mật:</strong> Nếu bạn KHÔNG yêu cầu đặt lại mật khẩu, vui lòng bỏ qua email này và xem xét thay đổi mật khẩu của bạn ngay lập tức để bảo vệ tài khoản.
                        </div>
                        """,
                otpCode, expirationMinutes);

        return templateService.buildEmail("Đặt lại mật khẩu - BidStorm", content);
    }
}
