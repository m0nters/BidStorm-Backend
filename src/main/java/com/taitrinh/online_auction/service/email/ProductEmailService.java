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
 * Email service for product lifecycle notifications
 * Handles product end events, winner announcements, no-winner notifications
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ProductEmailService {

    private final JavaMailSender mailSender;
    private final EmailTemplateService templateService;

    @Value("${spring.mail.username}")
    private String fromEmail;

    /**
     * Send notification to seller when product ends without any bids
     * Requirement 6.1: Email seller when auction ends with no bids
     */
    @Async
    public void sendNoWinnerNotificationToSeller(String toEmail, String sellerName, String productTitle,
            String productSlug) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(fromEmail);
            helper.setTo(toEmail);
            helper.setSubject(
                    String.format("%s - Sản phẩm đã kết thúc: %s", templateService.getAppName(), productTitle));
            helper.setText(buildNoWinnerEmailContent(sellerName, productTitle, productSlug), true);

            mailSender.send(message);
            log.info("No-winner notification sent to seller: {}", toEmail);
        } catch (MessagingException | MailException e) {
            log.error("Failed to send no-winner notification to seller {}: {}", toEmail, e.getMessage());
        }
    }

    /**
     * Send winner notification to the winning bidder
     */
    @Async
    public void sendWinnerNotificationToBidder(String toEmail, String bidderName, String productTitle,
            BigDecimal winningBid, String productSlug) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(fromEmail);
            helper.setTo(toEmail);
            helper.setSubject(String.format("%s - Chúc mừng! Bạn đã thắng đấu giá: %s", templateService.getAppName(),
                    productTitle));
            helper.setText(buildWinnerToBidderEmailContent(bidderName, productTitle, winningBid, productSlug), true);

            mailSender.send(message);
            log.info("Winner notification sent to bidder: {}", toEmail);
        } catch (MessagingException | MailException e) {
            log.error("Failed to send winner notification to bidder {}: {}", toEmail, e.getMessage());
        }
    }

    /**
     * Send winner notification to the seller with winner information
     */
    @Async
    public void sendWinnerNotificationToSeller(String toEmail, String sellerName, String productTitle,
            String winnerName, BigDecimal winningBid, String productSlug) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(fromEmail);
            helper.setTo(toEmail);
            helper.setSubject(String.format("%s - Sản phẩm đã bán: %s", templateService.getAppName(), productTitle));
            helper.setText(
                    buildWinnerToSellerEmailContent(sellerName, productTitle, winnerName, winningBid, productSlug),
                    true);

            mailSender.send(message);
            log.info("Winner notification sent to seller: {}", toEmail);
        } catch (MessagingException | MailException e) {
            log.error("Failed to send winner notification to seller {}: {}", toEmail, e.getMessage());
        }
    }

    // ========== Private Content Builders ==========

    private String buildNoWinnerEmailContent(String sellerName, String productTitle, String productSlug) {
        String productUrl = templateService.buildProductUrl(productSlug);

        String content = String.format(
                """
                        <p class="greeting">Xin chào %s,</p>

                        <div class="info-box">
                            <strong>📢 Sản phẩm của bạn đã kết thúc đấu giá</strong>
                        </div>

                        <div class="product-details">
                            <p><strong>Sản phẩm:</strong> %s</p>
                            <p><strong>Kết quả:</strong> Không có người đặt giá</p>
                        </div>

                        <p class="message">Rất tiếc, không có ai đặt giá cho sản phẩm này trong thời gian đấu giá. Bạn có thể đăng lại sản phẩm với giá khởi điểm thấp hơn hoặc điều chỉnh mô tả để thu hút nhiều người mua hơn.</p>

                        %s
                        """,
                sellerName, productTitle,
                templateService.buildButton("Xem sản phẩm", productUrl));

        return templateService.buildEmail("Sản phẩm đã kết thúc - BidStorm", content);
    }

    private String buildWinnerToBidderEmailContent(String bidderName, String productTitle,
            BigDecimal winningBid, String productSlug) {
        String productUrl = templateService.buildProductUrl(productSlug);

        String content = String.format(
                """
                        <p class="greeting">Xin chào %s,</p>

                        <div class="success-box">
                            <strong>🎉 Chúc mừng! Bạn đã thắng đấu giá!</strong>
                        </div>

                        <div class="product-details">
                            <p><strong>Sản phẩm:</strong> %s</p>
                            <p><strong>Giá thắng:</strong> %,d VND</p>
                        </div>

                        <p class="message">Người bán sẽ sớm liên hệ với bạn để hoàn tất giao dịch. Vui lòng kiểm tra thông tin liên lạc hoặc truy cập trang sản phẩm để biết thêm chi tiết.</p>

                        %s
                        """,
                bidderName, productTitle, winningBid.longValue(),
                templateService.buildButton("Xem sản phẩm", productUrl));

        return templateService.buildEmail("Chúc mừng bạn đã thắng! - BidStorm", content);
    }

    private String buildWinnerToSellerEmailContent(String sellerName, String productTitle,
            String winnerName, BigDecimal winningBid, String productSlug) {
        String productUrl = templateService.buildProductUrl(productSlug);

        String content = String.format(
                """
                        <p class="greeting">Xin chào %s,</p>

                        <div class="success-box">
                            <strong>🎉 Sản phẩm của bạn đã bán thành công!</strong>
                        </div>

                        <div class="product-details">
                            <p><strong>Sản phẩm:</strong> %s</p>
                            <p><strong>Người thắng:</strong> %s</p>
                            <p><strong>Giá bán:</strong> %,d VND</p>
                        </div>

                        <p class="message">Vui lòng liên hệ với người mua để hoàn tất giao dịch. Bạn có thể xem thông tin liên lạc của người mua trên trang sản phẩm.</p>

                        %s
                        """,
                sellerName, productTitle, winnerName, winningBid.longValue(),
                templateService.buildButton("Xem sản phẩm", productUrl));

        return templateService.buildEmail("Sản phẩm đã bán - BidStorm", content);
    }
}
