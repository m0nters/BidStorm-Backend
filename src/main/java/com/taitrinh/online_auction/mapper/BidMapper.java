package com.taitrinh.online_auction.mapper;

import java.util.List;
import java.util.stream.Collectors;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

import com.taitrinh.online_auction.dto.bid.BidResponse;
import com.taitrinh.online_auction.entity.BidHistory;
import com.taitrinh.online_auction.util.NameMaskingUtil;

@Mapper(componentModel = "spring")
public interface BidMapper {

    /**
     * Base mapping - masks bidder name by default
     */
    @Mapping(target = "productId", source = "product.id")
    @Mapping(target = "bidderId", source = "bidder.id")
    @Mapping(target = "bidderName", source = "bidder", qualifiedByName = "maskBidderName")
    @Mapping(target = "isYourself", ignore = true)
    @Mapping(target = "isHighestBidder", ignore = true) // Set in toResponseWithViewer
    BidResponse toResponse(BidHistory bidHistory);

    /**
     * Map bid with viewer context for name masking
     * Same pattern as CommentMapper - no separate fullBidderName field
     * 
     * @param bid             BidHistory entity
     * @param viewerId        ID of user viewing (null if not authenticated)
     * @param isProductSeller True if viewer is the product seller
     * @return BidResponse with appropriate name masking
     */
    default BidResponse toResponseWithViewer(BidHistory bid, Long viewerId, boolean isProductSeller) {
        BidResponse response = toResponse(bid);

        // Calculate flags
        boolean isYourself = viewerId != null && bid.getBidder() != null &&
                viewerId.equals(bid.getBidder().getId());

        // Set flags
        response.setIsYourself(isYourself);
        // Note: isHighestBidder is set externally (requires comparison with other bids)

        // Unmask bidderName for: product seller viewing their own product OR own bid
        if (bid.getBidder() != null) {
            if (isProductSeller || isYourself) {
                response.setBidderName(bid.getBidder().getFullName());
            }
            // else: keep masked (already set by base mapper)
        }

        // Only show maxBidAmount for: own bids OR product seller viewing their own
        // product
        if (!isYourself && !isProductSeller) {
            response.setMaxBidAmount(null); // Hide max bid from public
        }

        return response;
    }

    /**
     * Map list of bids with viewer context
     */
    default List<BidResponse> toResponseListWithViewer(List<BidHistory> bids, Long viewerId, boolean isProductSeller) {
        return bids.stream()
                .map(bid -> toResponseWithViewer(bid, viewerId, isProductSeller))
                .collect(Collectors.toList());
    }

    /**
     * Mask bidder name for privacy using centralized utility
     */
    @Named("maskBidderName")
    default String maskBidderName(com.taitrinh.online_auction.entity.User bidder) {
        if (bidder == null) {
            return null;
        }
        return NameMaskingUtil.maskName(bidder.getFullName());
    }
}
