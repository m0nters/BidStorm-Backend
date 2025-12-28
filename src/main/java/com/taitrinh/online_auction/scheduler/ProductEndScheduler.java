package com.taitrinh.online_auction.scheduler;

import java.time.ZonedDateTime;
import java.util.List;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.taitrinh.online_auction.entity.BidHistory;
import com.taitrinh.online_auction.entity.Product;
import com.taitrinh.online_auction.repository.BidHistoryRepository;
import com.taitrinh.online_auction.repository.ProductRepository;
import com.taitrinh.online_auction.service.email.ProductEmailService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Scheduled job to process product end events
 * Runs every minute to check for products that have ended
 * and send appropriate email notifications
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class ProductEndScheduler {

    private final ProductRepository productRepository;
    private final BidHistoryRepository bidHistoryRepository;
    private final ProductEmailService productEmailService;

    /**
     * Process products that ended in the last minute
     * Runs every 1 minute (cron: second, minute, hour, day, month, day-of-week)
     * Pattern: "0 * * * * *" = at second 0 of every minute
     */
    @Scheduled(cron = "0 * * * * *")
    @Transactional
    public void processEndedProducts() {
        ZonedDateTime now = ZonedDateTime.now();
        ZonedDateTime oneMinuteAgo = now.minusMinutes(1);

        log.debug("Checking for products that ended between {} and {}", oneMinuteAgo, now);

        // Find products that ended in the last minute
        List<Product> endedProducts = productRepository.findByIsEndedFalseAndEndTimeBetween(oneMinuteAgo, now);

        if (endedProducts.isEmpty()) {
            log.debug("No products ended in the last minute");
            return;
        }

        log.info("Processing {} ended product(s)", endedProducts.size());

        for (Product product : endedProducts) {
            try {
                processEndedProduct(product);
            } catch (Exception e) {
                log.error("Error processing ended product {}: {}", product.getId(), e.getMessage(), e);
                // Continue processing other products even if one fails
            }
        }
    }

    private void processEndedProduct(Product product) {
        // Mark product as ended
        product.setIsEnded(true);
        productRepository.save(product);

        log.info("Product {} marked as ended", product.getId());

        // Check if there are any bids
        bidHistoryRepository.findTopByProductIdOrderByBidAmountDesc(product.getId())
                .ifPresentOrElse(
                        winningBid -> handleProductWithWinner(product, winningBid),
                        () -> handleProductWithoutWinner(product));
    }

    private void handleProductWithWinner(Product product, BidHistory winningBid) {
        log.info("Product {} ended with winner: {} (bid: {})",
                product.getId(),
                winningBid.getBidder().getFullName(),
                winningBid.getBidAmount());

        // Send winner email to bidder
        productEmailService.sendWinnerNotificationToBidder(
                winningBid.getBidder().getEmail(),
                winningBid.getBidder().getFullName(),
                product.getTitle(),
                winningBid.getBidAmount(),
                product.getSlug());

        // Send winner email to seller
        productEmailService.sendWinnerNotificationToSeller(
                product.getSeller().getEmail(),
                product.getSeller().getFullName(),
                product.getTitle(),
                winningBid.getBidder().getFullName(),
                winningBid.getBidAmount(),
                product.getSlug());

        log.info("Winner notifications sent for product {}", product.getId());
    }

    private void handleProductWithoutWinner(Product product) {
        log.info("Product {} ended without any bids", product.getId());

        // Send no-winner email to seller (requirement 6.1)
        productEmailService.sendNoWinnerNotificationToSeller(
                product.getSeller().getEmail(),
                product.getSeller().getFullName(),
                product.getTitle(),
                product.getSlug());

        log.info("No-winner notification sent to seller for product {}", product.getId());
    }
}
