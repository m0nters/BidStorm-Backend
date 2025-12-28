package com.taitrinh.online_auction.service.email;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * Shared email template utilities and HTML builders
 * Provides common styling and structure for all email services
 */
@Service
public class EmailTemplateService {

    @Value("${app.name:BidStorm}")
    private String appName;

    @Value("${app.url:http://localhost:3000}")
    private String appUrl;

    /**
     * Get common CSS styles used across all emails
     */
    public String getCommonStyles() {
        return """
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
                .warning-box {
                    background-color: #fff3cd;
                    border-left: 4px solid #ffc107;
                    padding: 15px 20px;
                    margin: 30px 0;
                    text-align: left;
                    font-size: 14px;
                }
                .danger-box {
                    background-color: #f8d7da;
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
                .bid-details, .product-details {
                    background-color: #f8f8f8;
                    padding: 20px;
                    border-radius: 8px;
                    margin: 20px 0;
                }
                .bid-details p, .product-details p {
                    margin: 10px 0;
                    font-size: 15px;
                }
                .bid-details strong, .product-details strong {
                    color: #000000;
                }
                """;
    }

    /**
     * Build email header HTML
     */
    public String buildHeader(String title) {
        return String.format("""
                <div class="header">
                    <h1>%s</h1>
                </div>
                """, title != null ? title : appName);
    }

    /**
     * Build email footer HTML
     */
    public String buildFooter() {
        return String.format(
                """
                        <div class="footer">
                            <p>© 2025 %s. All rights reserved.</p>
                            <p>Email tự động, vui lòng không trả lời. Nếu cần hỗ trợ, liên hệ <a href="mailto:support@bidstorm.com">support@bidstorm.com</a></p>
                        </div>
                        """,
                appName);
    }

    /**
     * Build a button link
     */
    public String buildButton(String text, String url) {
        return String.format(
                """
                        <p style="text-align: center; margin: 30px 0;">
                            <a href="%s" style="display: inline-block; padding: 15px 30px; background-color: #000000; color: #ffffff; text-decoration: none; border-radius: 8px; font-weight: bold; font-size: 16px;">%s</a>
                        </p>
                        """,
                url, text);
    }

    /**
     * Build complete email wrapper
     */
    public String buildEmail(String title, String contentHtml) {
        return String.format("""
                <!DOCTYPE html>
                <html lang="vi">
                <head>
                    <meta charset="UTF-8">
                    <meta name="viewport" content="width=device-width, initial-scale=1.0">
                    <title>%s</title>
                    <style>
                        %s
                    </style>
                </head>
                <body>
                    <div class="container">
                        %s
                        <div class="content">
                            %s
                        </div>
                        %s
                    </div>
                </body>
                </html>
                """, title, getCommonStyles(), buildHeader(null), contentHtml, buildFooter());
    }

    /**
     * Build product URL
     */
    public String buildProductUrl(String productSlug) {
        return String.format("%s/san-pham/%s", appUrl, productSlug);
    }

    /**
     * Build comment URL with anchor
     */
    public String buildCommentUrl(String productSlug, Long commentId) {
        return String.format("%s/san-pham/%s#comment-%d", appUrl, productSlug, commentId);
    }

    public String getAppName() {
        return appName;
    }

    public String getAppUrl() {
        return appUrl;
    }
}
