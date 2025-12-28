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
 * Email service for comment/Q&A notifications
 * Handles seller questions, replies, and product activity notifications
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class CommentEmailService {

    private final JavaMailSender mailSender;
    private final EmailTemplateService templateService;

    @Value("${spring.mail.username}")
    private String fromEmail;

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
            helper.setSubject(
                    String.format("%s - Câu hỏi mới về sản phẩm: %s", templateService.getAppName(), productTitle));
            helper.setText(buildNewQuestionEmailContent(sellerName, productTitle, askerName, questionText, productSlug,
                    commentId), true);

            mailSender.send(message);
            log.info("New question notification sent to seller: {}", toEmail);
        } catch (MessagingException | MailException e) {
            log.error("Failed to send new question notification to seller {}: {}", toEmail, e.getMessage());
        }
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
            helper.setSubject(String.format("%s - Người bán đã trả lời câu hỏi của bạn: %s",
                    templateService.getAppName(), productTitle));
            helper.setText(
                    buildSellerReplyEmailContent(participantName, productTitle, replyText, productSlug, commentId),
                    true);

            mailSender.send(message);
            log.info("Seller reply notification sent to: {}", toEmail);
        } catch (MessagingException | MailException e) {
            log.error("Failed to send seller reply notification to {}: {}", toEmail, e.getMessage());
        }
    }

    /**
     * Send general product activity notification
     */
    @Async
    public void sendProductActivityNotification(String toEmail, String participantName, String productTitle,
            String activityText, String productSlug, Long commentId) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(fromEmail);
            helper.setTo(toEmail);
            helper.setSubject(
                    String.format("%s - Hoạt động mới trên sản phẩm: %s", templateService.getAppName(), productTitle));
            helper.setText(buildProductActivityEmailContent(participantName, productTitle, activityText, productSlug,
                    commentId), true);

            mailSender.send(message);
            log.info("Product activity notification sent to: {}", toEmail);
        } catch (MessagingException | MailException e) {
            log.error("Failed to send product activity notification to {}: {}", toEmail, e.getMessage());
        }
    }

    // ========== Private Content Builders ==========

    private String buildNewQuestionEmailContent(String sellerName, String productTitle,
            String askerName, String questionText, String productSlug, Long commentId) {
        String questionUrl = templateService.buildCommentUrl(productSlug, commentId);
        String truncatedQuestion = questionText.length() > 200
                ? questionText.substring(0, 200) + "..."
                : questionText;

        String content = String.format(
                """
                        <p class="greeting">Xin chào %s,</p>

                        <div class="info-box">
                            <strong>💬 Có câu hỏi mới về sản phẩm của bạn!</strong>
                        </div>

                        <div class="product-details">
                            <p><strong>Sản phẩm:</strong> %s</p>
                            <p><strong>Người hỏi:</strong> %s</p>
                        </div>

                        <div class="message" style="background-color: #f8f8f8; padding: 20px; border-radius: 8px; margin: 20px 0;">
                            <p style="margin: 0; font-style: italic; color: #555;">"%s"</p>
                        </div>

                        <p class="message">Vui lòng trả lời câu hỏi để giúp người mua hiểu rõ hơn về sản phẩm của bạn.</p>

                        %s
                        """,
                sellerName, productTitle, askerName, truncatedQuestion,
                templateService.buildButton("Trả lời câu hỏi", questionUrl));

        return templateService.buildEmail("Câu hỏi mới - BidStorm", content);
    }

    private String buildSellerReplyEmailContent(String participantName, String productTitle,
            String replyText, String productSlug, Long commentId) {
        String commentUrl = templateService.buildCommentUrl(productSlug, commentId);
        String truncatedReply = replyText.length() > 200
                ? replyText.substring(0, 200) + "..."
                : replyText;

        String content = String.format(
                """
                        <p class="greeting">Xin chào %s,</p>

                        <div class="success-box">
                            <strong>✅ Người bán đã trả lời câu hỏi!</strong>
                        </div>

                        <div class="product-details">
                            <p><strong>Sản phẩm:</strong> %s</p>
                        </div>

                        <div class="message" style="background-color: #f8f8f8; padding: 20px; border-radius: 8px; margin: 20px 0;">
                            <p style="margin: 0; font-style: italic; color: #555;">"%s"</p>
                        </div>

                        %s
                        """,
                participantName, productTitle, truncatedReply,
                templateService.buildButton("Xem câu trả lời", commentUrl));

        return templateService.buildEmail("Người bán đã trả lời - BidStorm", content);
    }

    private String buildProductActivityEmailContent(String participantName, String productTitle,
            String activityText, String productSlug, Long commentId) {
        String commentUrl = templateService.buildCommentUrl(productSlug, commentId);

        String content = String.format("""
                <p class="greeting">Xin chào %s,</p>

                <div class="info-box">
                    <strong>🔔 Hoạt động mới trên sản phẩm bạn quan tâm!</strong>
                </div>

                <div class="product-details">
                    <p><strong>Sản phẩm:</strong> %s</p>
                </div>

                <p class="message">%s</p>

                %s
                """, participantName, productTitle, activityText,
                templateService.buildButton("Xem chi tiết", commentUrl));

        return templateService.buildEmail("Hoạt động mới - BidStorm", content);
    }
}
