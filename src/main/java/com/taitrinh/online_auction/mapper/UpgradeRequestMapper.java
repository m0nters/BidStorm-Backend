package com.taitrinh.online_auction.mapper;

import org.springframework.stereotype.Component;

import com.taitrinh.online_auction.dto.admin.UpgradeRequestResponse;
import com.taitrinh.online_auction.entity.UpgradeRequest;
import com.taitrinh.online_auction.entity.User;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class UpgradeRequestMapper {

    /**
     * Map UpgradeRequest entity to UpgradeRequestResponse DTO
     */
    public UpgradeRequestResponse toUpgradeRequestResponse(UpgradeRequest request) {
        User bidder = request.getBidder();
        User admin = request.getAdmin();

        return UpgradeRequestResponse.builder()
                .id(request.getId())
                .bidderId(bidder.getId())
                .bidderName(bidder.getFullName())
                .bidderEmail(bidder.getEmail())
                .bidderPositiveRating(bidder.getPositiveRating())
                .bidderNegativeRating(bidder.getNegativeRating())
                .reason(request.getReason())
                .status(request.getStatus().name())
                .adminId(admin != null ? admin.getId() : null)
                .adminName(admin != null ? admin.getFullName() : null)
                .reviewedAt(request.getReviewedAt())
                .createdAt(request.getCreatedAt())
                .build();
    }
}
