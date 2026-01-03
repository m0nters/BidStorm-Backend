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
@Schema(description = "Revenue statistics for admin dashboard")
public class RevenueStatisticsResponse {

    @Schema(description = "Total revenue from completed orders (in smallest currency unit, e.g., cents or VND)", example = "1000000000")
    private Long totalRevenueCents;

    @Schema(description = "Total number of completed orders", example = "150")
    private Long completedOrderCount;

    @Schema(description = "Currency code", example = "VND")
    private String currency;

    @Schema(description = "Average order value in cents", example = "6666666")
    private Long averageOrderValueCents;
}
