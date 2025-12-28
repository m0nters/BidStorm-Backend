package com.taitrinh.online_auction.service.email;

import java.math.BigDecimal;

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
 * Email service for bid-related notifications
 * Handles bid confirmations, outbid alerts, and seller notifications
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class BidEmailService {

    private final JavaMailSender mailSender;
    private final EmailTemplateService templateService;

    @Value("${spring.mail.username}")
    private String fromEmail;

    /**
     * Send bid confirmation email to bidder
     */
    @Async
    public void sendBidConfirmationToBidder(String toEmail, String bidderName, String productTitle,
            BigDecimal bidAmount, boolean isWinning, String productSlug) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(fromEmail);
            helper.setTo(toEmail);
            helper.setSubject(
                    String.format("%s - Xác nhận đấu giá sản phẩm: %s", templateService.getAppName(), productTitle));
            helper.setText(
                    buildBidConfirmationEmailContent(bidderName, productTitle, bidAmount, isWinning, productSlug),
                    true);

            mailSender.send(message);
            log.info("Bid confirmation email sent to: {}", toEmail);
        } catch (MessagingException | MailException e) {
            log.error("Failed to send bid confirmation email to {}: {}", toEmail, e.getMessage());
        }
    }

    /**
     * Send notification to seller about new bid
     */
    @Async
    public void sendNewBidNotificationToSeller(String toEmail, String sellerName, String productTitle,
            String bidderName, BigDecimal newPrice, String productSlug) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(fromEmail);
            helper.setTo(toEmail);
            helper.setSubject(String.format("%s - Lượt đấu giá mới cho sản phẩm: %s", templateService.getAppName(),
                    productTitle));
            helper.setText(buildNewBidToSellerEmailContent(sellerName, productTitle, bidderName, newPrice, productSlug),
                    true);

            mailSender.send(message);
            log.info("New bid notification sent to seller: {}", toEmail);
        } catch (MessagingException | MailException e) {
            log.error("Failed to send new bid notification to seller {}: {}", toEmail, e.getMessage());
        }
    }

    /**
     * Send outbid notification to previous highest bidder
     */
    @Async
    public void sendOutbidNotification(String toEmail, String bidderName, String productTitle,
            BigDecimal newPrice, String productSlug) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(fromEmail);
            helper.setTo(toEmail);
            helper.setSubject(String.format("%s - Bạn đã bị trả giá cao hơn ở sản phẩm: %s",
                    templateService.getAppName(), productTitle));
            helper.setText(buildOutbidEmailContent(bidderName, productTitle, newPrice, productSlug), true);

            mailSender.send(message);
            log.info("Outbid notification sent to: {}", toEmail);
        } catch (MessagingException | MailException e) {
            log.error("Failed to send outbid notification to {}: {}", toEmail, e.getMessage());
        }
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
            helper.setSubject(String.format("%s - Đấu giá bị từ chối cho sản phẩm: %s", templateService.getAppName(),
                    productTitle));
            helper.setText(buildBidRejectionEmailContent(bidderName, productTitle, productSlug), true);

            mailSender.send(message);
            log.info("Bid rejection email sent to: {}", toEmail);
        } catch (MessagingException | MailException e) {
            log.error("Failed to send bid rejection email to {}: {}", toEmail, e.getMessage());
        }
    }

    // ========== Private Content Builders ==========

    private String buildBidConfirmationEmailContent(String bidderName, String productTitle,
            BigDecimal bidAmount, boolean isWinning, String productSlug) {
        String productUrl = templateService.buildProductUrl(productSlug);
        String statusMessage = isWinning
                ? "Bạn hiện đang là người đặt giá cao nhất! 🎉"
                : "Lượt đấu giá của bạn đã được ghi nhận thành công.";
        String statusClass = isWinning ? "success-box" : "info-box";

        String content = String.format("""
                <p class="greeting">Xin chào %s,</p>

                <div class="%s">
                    <strong>%s</strong>
                </div>

                <div class="bid-details">
                    <p><strong>Sản phẩm:</strong> %s</p>
                    <p><strong>Giá đấu tối đa của bạn:</strong> %,d VND</p>
                </div>

                <p class="message">Chúng tôi sẽ thông báo cho bạn nếu có người đặt giá cao hơn.</p>

                %s
                """, bidderName, statusClass, statusMessage, productTitle, bidAmount.longValue(),
                templateService.buildButton("Đi tới sản phẩm", productUrl));

        return templateService.buildEmail("Xác nhận đấu giá - BidStorm", content);
    }

    private String buildNewBidToSellerEmailContent(String sellerName, String productTitle,
            String bidderName, BigDecimal newPrice, String productSlug) {
        String productUrl = templateService.buildProductUrl(productSlug);

        String content = String.format("""
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

                %s
                """, sellerName, productTitle, bidderName, newPrice.longValue(),
                templateService.buildButton("Đi tới sản phẩm", productUrl));

        return templateService.buildEmail("Lượt đấu giá mới - BidStorm", content);
    }

    private String buildOutbidEmailContent(String bidderName, String productTitle,
            BigDecimal newPrice, String productSlug) {
        String productUrl = templateService.buildProductUrl(productSlug);

        String content = String.format("""
                <p class="greeting">Xin chào %s,</p>

                <div class="warning-box">
                    <strong>⚠️ Có người đã đặt giá cao hơn bạn!</strong>
                </div>

                <div class="bid-details">
                    <p><strong>Sản phẩm:</strong> %s</p>
                    <p><strong>Giá hiện tại:</strong> %,d VND</p>
                </div>

                <p class="message">Đặt giá mới ngay để tiếp tục tham gia đấu giá!</p>

                %s
                """, bidderName, productTitle, newPrice.longValue(),
                templateService.buildButton("Đi tới sản phẩm", productUrl));

        return templateService.buildEmail("Bạn đã bị vượt giá - BidStorm", content);
    }

    private String buildBidRejectionEmailContent(String bidderName, String productTitle, String productSlug) {
        String content = String.format("""
                <p class="greeting">Xin chào %s,</p>

                <div class="danger-box">
                    <strong>❌ Người bán đã từ chối lượt đấu giá của bạn</strong>
                </div>

                <div class="product-details">
                    <p><strong>Sản phẩm:</strong> %s</p>
                </div>

                <p class="message">Bạn không còn được phép tham gia đấu giá sản phẩm này.</p>
                <p class="message">Nếu có thắc mắc, vui lòng liên hệ với người bán hoặc bộ phận hỗ trợ.</p>
                """, bidderName, productTitle);

        return templateService.buildEmail("Lượt đấu giá bị từ chối - BidStorm", content);
    }
}
