package com.taitrinh.online_auction.dto.bid;

import java.math.BigDecimal;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BidRequest {

    @NotNull(message = "Max bid amount is required")
    @DecimalMin(value = "0.0", inclusive = false, message = "Max bid amount must be greater than 0")
    private BigDecimal maxBidAmount; // User's maximum willing to pay
}
