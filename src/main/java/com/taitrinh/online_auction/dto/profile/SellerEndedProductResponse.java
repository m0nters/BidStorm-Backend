package com.taitrinh.online_auction.dto.profile;

import java.math.BigDecimal;
import java.time.ZonedDateTime;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Seller's ended product with winner information")
public class SellerEndedProductResponse {

    @Schema(description = "Product ID", example = "1")
    private Long productId;

    @Schema(description = "Product title", example = "iPhone 15 Pro Max")
    private String title;

    @Schema(description = "Product URL slug", example = "iphone-15-pro-max-256gb")
    private String slug;

    @Schema(description = "Product thumbnail URL", example = "https://example.com/image.jpg")
    private String thumbnailUrl;

    @Schema(description = "Product starting price", example = "10000000")
    private BigDecimal startingPrice;

    @Schema(description = "Final winning price", example = "25000000")
    private BigDecimal finalPrice;

    @Schema(description = "Auction end time")
    private ZonedDateTime endTime;

    @Schema(description = "Winner user ID", example = "5")
    private Long winnerId;

    @Schema(description = "Winner full name", example = "Nguyễn Văn A")
    private String winnerName;

    @Schema(description = "Winner positive rating count", example = "10")
    private Integer winnerPositiveRating;

    @Schema(description = "Winner negative rating count", example = "2")
    private Integer winnerNegativeRating;

    @Schema(description = "Has seller reviewed this winner?", example = "false")
    private Boolean hasReviewed;

    @Schema(description = "Order status if order exists", example = "PENDING_PAYMENT")
    private String orderStatus;

    @Schema(description = "Order completion ID if order exists", example = "1")
    private Long orderId;
}
