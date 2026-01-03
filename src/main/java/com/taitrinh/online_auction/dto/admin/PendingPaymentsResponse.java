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
@Schema(description = "Pending payments statistics")
public class PendingPaymentsResponse {

    @Schema(description = "Total pending payment amount in cents", example = "450000000")
    private Long totalPendingCents;

    @Schema(description = "Number of pending payment orders", example = "12")
    private Long orderCount;

    @Schema(description = "Currency code", example = "VND")
    private String currency;
}
