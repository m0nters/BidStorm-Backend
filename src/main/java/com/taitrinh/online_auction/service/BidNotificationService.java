package com.taitrinh.online_auction.service;

import java.math.BigDecimal;
import java.time.ZonedDateTime;

import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import com.taitrinh.online_auction.dto.bid.BidResponse;
import com.taitrinh.online_auction.dto.websocket.BidEvent;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class BidNotificationService {

    private final SimpMessagingTemplate messagingTemplate;

    /**
     * Notify all subscribers about a new bid (public channel - masked names)
     */
    public void notifyNewBid(Long productId, BidResponse bid, BigDecimal currentPrice, String highestBidder,
            ZonedDateTime endTime) {
        log.debug("Broadcasting new bid for product: {} bidder: {}", productId, bid.getBidderId());

        BidEvent event = BidEvent.newBid(productId, bid, currentPrice, highestBidder, endTime);
        String destination = "/topic/product/" + productId + "/bids";

        messagingTemplate.convertAndSend(destination, event);

        log.info("Broadcasted new bid to public channel: {}", destination);
    }

    /**
     * Notify product seller about a new bid (seller channel - unmasked names)
     */
    public void notifyNewBidToSeller(Long productId, BidResponse bid, BigDecimal currentPrice, String highestBidder,
            ZonedDateTime endTime) {
        log.debug("Broadcasting new bid to seller for product: {} bidder: {}", productId, bid.getBidderId());

        BidEvent event = BidEvent.newBid(productId, bid, currentPrice, highestBidder, endTime);
        String destination = "/topic/product/" + productId + "/bids/seller";

        messagingTemplate.convertAndSend(destination, event);

        log.info("Broadcasted new bid to seller channel: {}", destination);
    }

    /**
     * Notify bidder that they were rejected (both channels)
     * Includes updated auction state (new price and highest bidder)
     */
    public void notifyBidRejected(Long productId, Long bidderId, BigDecimal newCurrentPrice,
            String newHighestBidder) {
        log.debug("Broadcasting bid rejection for product: {} bidder: {}", productId, bidderId);

        BidEvent event = BidEvent.bidRejected(productId, bidderId);
        event.setCurrentPrice(newCurrentPrice);
        event.setHighestBidder(newHighestBidder);

        // Broadcast to both public and seller channels
        String publicDestination = "/topic/product/" + productId + "/bids";
        String sellerDestination = "/topic/product/" + productId + "/bids/seller";

        messagingTemplate.convertAndSend(publicDestination, event);
        messagingTemplate.convertAndSend(sellerDestination, event);

        log.info("Broadcasted bid rejection to both channels");
    }
}
