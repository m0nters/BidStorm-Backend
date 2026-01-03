package com.taitrinh.online_auction.dto.admin;

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
@Schema(description = "Upgrade request information for admin review")
public class UpgradeRequestResponse {

    @Schema(description = "Request ID", example = "1")
    private Long id;

    @Schema(description = "Bidder user ID", example = "5")
    private Long bidderId;

    @Schema(description = "Bidder full name", example = "Nguyễn Văn A")
    private String bidderName;

    @Schema(description = "Bidder email", example = "bidder@example.com")
    private String bidderEmail;

    @Schema(description = "Bidder positive rating", example = "10")
    private Integer bidderPositiveRating;

    @Schema(description = "Bidder negative rating", example = "2")
    private Integer bidderNegativeRating;

    @Schema(description = "Reason for upgrade request")
    private String reason;

    @Schema(description = "Request status (PENDING, APPROVED, REJECTED)", example = "PENDING")
    private String status;

    @Schema(description = "Admin who reviewed this request (if reviewed)")
    private Long adminId;

    @Schema(description = "Admin name who reviewed this request")
    private String adminName;

    @Schema(description = "Review timestamp")
    private ZonedDateTime reviewedAt;

    @Schema(description = "Request creation date")
    private ZonedDateTime createdAt;
}
