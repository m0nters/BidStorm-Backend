package com.taitrinh.online_auction.dto.profile;

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
@Schema(description = "Review details with reviewer and product information")
public class ReviewResponse {

    @Schema(description = "Review ID", example = "1")
    private Long id;

    @Schema(description = "Product ID", example = "1")
    private Long productId;

    @Schema(description = "Product title", example = "iPhone 13 Pro Max")
    private String productTitle;

    @Schema(description = "Reviewer ID", example = "2")
    private Long reviewerId;

    @Schema(description = "Reviewer name (masked if necessary)", example = "****John")
    private String reviewerName;

    @Schema(description = "Rating: 1 for positive, -1 for negative", example = "1")
    private Short rating;

    @Schema(description = "Review comment", example = "Great seller!")
    private String comment;

    @Schema(description = "Review creation date")
    private ZonedDateTime createdAt;
}
