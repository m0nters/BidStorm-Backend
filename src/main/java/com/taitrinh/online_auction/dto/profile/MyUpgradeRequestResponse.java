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
@Schema(description = "User's own upgrade request status")
public class MyUpgradeRequestResponse {

    @Schema(description = "Request ID", example = "1")
    private Long id;

    @Schema(description = "Reason for upgrade request")
    private String reason;

    @Schema(description = "Request status (PENDING, APPROVED, REJECTED)", example = "PENDING")
    private String status;

    @Schema(description = "Admin name who reviewed (if reviewed)")
    private String adminName;

    @Schema(description = "Review timestamp")
    private ZonedDateTime reviewedAt;

    @Schema(description = "Request creation date")
    private ZonedDateTime createdAt;
}
