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
@Schema(description = "Basic count statistics (new auctions, users, upgrades, zero-bid products)")
public class BasicStatisticsResponse {

    @Schema(description = "Number of new auction listings created in the time period", example = "45")
    private Long newAuctionListings;

    @Schema(description = "Number of new users registered in the time period", example = "127")
    private Long newUsers;

    @Schema(description = "Number of approved seller upgrades in the time period", example = "8")
    private Long newSellerUpgrades;

    @Schema(description = "Number of products with zero bids", example = "23")
    private Long zeroBidProducts;
}
