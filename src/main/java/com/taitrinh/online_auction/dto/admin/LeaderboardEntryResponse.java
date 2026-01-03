package com.taitrinh.online_auction.dto.admin;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Leaderboard entry for top bidders or sellers")
public class LeaderboardEntryResponse {

    @Schema(description = "User ID", example = "42")
    private Long userId;

    @Schema(description = "User full name", example = "Nguyen Van A")
    private String fullName;

    @Schema(description = "User email", example = "nguyenvana@example.com")
    private String email;

    @Schema(description = "Total value (spend for bidders, revenue for sellers) in cents", example = "120000000")
    private Long valueCents;

    @Schema(description = "Count (bids for bidders, products for sellers)", example = "156")
    private Long count;
}
