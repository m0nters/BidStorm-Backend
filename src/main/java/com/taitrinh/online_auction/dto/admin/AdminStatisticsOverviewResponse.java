package com.taitrinh.online_auction.dto.admin;

import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Complete admin statistics overview")
public class AdminStatisticsOverviewResponse {

    @Schema(description = "Time period for the statistics", example = "LAST_30_DAYS")
    private String timePeriod;

    @Schema(description = "Basic count statistics")
    private BasicStatisticsResponse basicStatistics;

    @Schema(description = "Total revenue statistics")
    private RevenueStatisticsResponse totalRevenue;

    @Schema(description = "Revenue breakdown by category")
    private List<CategoryRevenueResponse> categoryRevenue;

    @Schema(description = "Pending payments statistics")
    private PendingPaymentsResponse pendingPayments;

    @Schema(description = "Top bidders leaderboard")
    private List<LeaderboardEntryResponse> topBidders;

    @Schema(description = "Top sellers leaderboard")
    private List<LeaderboardEntryResponse> topSellers;
}
