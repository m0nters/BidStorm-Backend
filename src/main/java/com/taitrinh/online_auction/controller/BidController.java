package com.taitrinh.online_auction.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.taitrinh.online_auction.dto.ApiResponse;
import com.taitrinh.online_auction.dto.bid.BidRequest;
import com.taitrinh.online_auction.dto.bid.BidResponse;
import com.taitrinh.online_auction.security.UserDetailsImpl;
import com.taitrinh.online_auction.service.BidService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
@Tag(name = "Bid History", description = "Automatic bidding API")
public class BidController {

    private final BidService bidService;

    @Operation(summary = "Place an automatic bid", description = "Place a bid with your maximum willing amount. System will automatically outbid others up to this amount.", security = @SecurityRequirement(name = "Bearer Authentication"))
    @PostMapping("/products/{productId}/bids")
    public ResponseEntity<ApiResponse<BidResponse>> placeBid(
            @PathVariable Long productId,
            @Valid @RequestBody BidRequest request,
            @AuthenticationPrincipal UserDetailsImpl userDetails) {

        BidResponse response = bidService.placeBid(productId, request, userDetails.getUserId());
        return ResponseEntity.status(201).body(ApiResponse.created(response, "Đấu giá thành công"));
    }

    @Operation(summary = "Get bid history for a product", description = "Returns bid history with masked names for privacy. Sellers see unmasked names. Users see their own bids with maxBidAmount.")
    @GetMapping("/products/{productId}/bids")
    public ResponseEntity<ApiResponse<List<BidResponse>>> getBidHistory(
            @PathVariable Long productId,
            @AuthenticationPrincipal UserDetailsImpl userDetails) {

        Long viewerId = userDetails != null ? userDetails.getUserId() : null;
        List<BidResponse> bids = bidService.getBidHistory(productId, viewerId);
        return ResponseEntity.ok(ApiResponse.ok(bids));
    }

    @Operation(summary = "[SELLER] Reject a bidder", description = "Remove all bids from a specific bidder. Only product seller can do this. Recalculates highest bidder.", security = @SecurityRequirement(name = "Bearer Authentication"))
    @DeleteMapping("/products/{productId}/bidders/{bidderId}")
    public ResponseEntity<ApiResponse<Void>> rejectBidder(
            @PathVariable Long productId,
            @PathVariable Long bidderId,
            @AuthenticationPrincipal UserDetailsImpl userDetails) {

        bidService.rejectBidder(productId, bidderId, userDetails.getUserId());
        return ResponseEntity.ok(ApiResponse.ok(null, "Đã từ chối người đấu giá thành công"));
    }

    @Operation(summary = "Buy product now", description = "Instantly purchase product at buy now price. Ends auction immediately and sets buyer as winner.", security = @SecurityRequirement(name = "Bearer Authentication"))
    @PostMapping("/products/{productId}/buy-now")
    public ResponseEntity<ApiResponse<BidResponse>> buyNow(
            @PathVariable Long productId,
            @AuthenticationPrincipal UserDetailsImpl userDetails) {

        BidResponse response = bidService.buyNow(productId, userDetails.getUserId());
        return ResponseEntity.ok(ApiResponse.ok(response, "Mua ngay thành công"));
    }
}
