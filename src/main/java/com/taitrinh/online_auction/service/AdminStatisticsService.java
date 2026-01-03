package com.taitrinh.online_auction.service;

import java.time.ZonedDateTime;
import java.util.List;

import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.taitrinh.online_auction.dto.admin.AdminStatisticsOverviewResponse;
import com.taitrinh.online_auction.dto.admin.BasicStatisticsResponse;
import com.taitrinh.online_auction.dto.admin.CategoryRevenueResponse;
import com.taitrinh.online_auction.dto.admin.LeaderboardEntryResponse;
import com.taitrinh.online_auction.dto.admin.PendingPaymentsResponse;
import com.taitrinh.online_auction.entity.OrderCompletion.OrderStatus;
import com.taitrinh.online_auction.entity.UpgradeRequest.UpgradeStatus;
import com.taitrinh.online_auction.enums.TimePeriod;
import com.taitrinh.online_auction.repository.BidHistoryRepository;
import com.taitrinh.online_auction.repository.CategoryRepository;
import com.taitrinh.online_auction.repository.OrderCompletionRepository;
import com.taitrinh.online_auction.repository.ProductRepository;
import com.taitrinh.online_auction.repository.UpgradeRequestRepository;
import com.taitrinh.online_auction.repository.UserRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class AdminStatisticsService {

    private final ProductRepository productRepository;
    private final UserRepository userRepository;
    private final UpgradeRequestRepository upgradeRequestRepository;
    private final OrderCompletionRepository orderCompletionRepository;
    private final CategoryRepository categoryRepository;
    private final BidHistoryRepository bidHistoryRepository;

    // ========== BASIC STATISTICS ==========

    /**
     * Get basic count statistics (new auctions, users, upgrades, zero-bid products)
     */
    @Transactional(readOnly = true)
    public BasicStatisticsResponse getBasicStatistics(TimePeriod period) {
        ZonedDateTime timestamp = getTimestampForPeriod(period);

        long newAuctionListings = period == TimePeriod.ALL_TIME
                ? productRepository.count()
                : productRepository.countByCreatedAtAfter(timestamp);

        long newUsers = period == TimePeriod.ALL_TIME
                ? userRepository.count()
                : userRepository.countByCreatedAtAfter(timestamp);

        long newSellerUpgrades = period == TimePeriod.ALL_TIME
                ? upgradeRequestRepository.countByStatus(UpgradeStatus.APPROVED)
                : upgradeRequestRepository.countByStatusAndReviewedAtAfter(UpgradeStatus.APPROVED, timestamp);

        long zeroBidProducts = productRepository.countZeroBidProducts();

        log.info("Basic statistics calculated for period {}: auctions={}, users={}, upgrades={}, zeroBid={}",
                period, newAuctionListings, newUsers, newSellerUpgrades, zeroBidProducts);

        return BasicStatisticsResponse.builder()
                .newAuctionListings(newAuctionListings)
                .newUsers(newUsers)
                .newSellerUpgrades(newSellerUpgrades)
                .zeroBidProducts(zeroBidProducts)
                .build();
    }

    // ========== FINANCIAL STATISTICS ==========

    /**
     * Get revenue breakdown by category
     */
    @Transactional(readOnly = true)
    public List<CategoryRevenueResponse> getRevenueByCategory() {
        List<CategoryRevenueResponse> revenue = categoryRepository.getRevenueByCategory();
        log.info("Revenue by category calculated: {} categories", revenue.size());
        return revenue;
    }

    /**
     * Get pending payments statistics
     */
    @Transactional(readOnly = true)
    public PendingPaymentsResponse getPendingPayments() {
        Long totalPendingCents = orderCompletionRepository.getTotalPendingPaymentsCents();
        long orderCount = orderCompletionRepository.countByStatus(OrderStatus.PENDING_PAYMENT);

        log.info("Pending payments calculated: total={} cents, count={}", totalPendingCents, orderCount);

        return PendingPaymentsResponse.builder()
                .totalPendingCents(totalPendingCents != null ? totalPendingCents : 0L)
                .orderCount(orderCount)
                .currency("VND")
                .build();
    }

    /**
     * Get overall revenue statistics (total revenue, order count, average)
     */
    @Transactional(readOnly = true)
    public com.taitrinh.online_auction.dto.admin.RevenueStatisticsResponse getRevenueStatistics() {
        Long totalRevenueCents = orderCompletionRepository.getTotalRevenueCents();
        long completedOrderCount = orderCompletionRepository.countByStatus(OrderStatus.COMPLETED);
        long averageOrderValueCents = completedOrderCount > 0 && totalRevenueCents != null
                ? totalRevenueCents / completedOrderCount
                : 0;

        log.info("Revenue statistics calculated: total={} cents, orders={}, avg={} cents",
                totalRevenueCents, completedOrderCount, averageOrderValueCents);

        return com.taitrinh.online_auction.dto.admin.RevenueStatisticsResponse.builder()
                .totalRevenueCents(totalRevenueCents != null ? totalRevenueCents : 0L)
                .completedOrderCount(completedOrderCount)
                .currency("VND")
                .averageOrderValueCents(averageOrderValueCents)
                .build();
    }

    // ========== LEADERBOARDS ==========

    /**
     * Get top bidders leaderboard
     */
    @Transactional(readOnly = true)
    public List<LeaderboardEntryResponse> getTopBidders(int limit) {
        List<LeaderboardEntryResponse> topBidders = bidHistoryRepository.getTopBidders(PageRequest.of(0, limit));
        log.info("Top {} bidders retrieved", topBidders.size());
        return topBidders;
    }

    /**
     * Get top sellers leaderboard
     */
    @Transactional(readOnly = true)
    public List<LeaderboardEntryResponse> getTopSellers(int limit) {
        List<LeaderboardEntryResponse> topSellers = bidHistoryRepository.getTopSellers(PageRequest.of(0, limit));
        log.info("Top {} sellers retrieved", topSellers.size());
        return topSellers;
    }

    // ========== COMBINED OVERVIEW ==========

    /**
     * Get complete statistics overview combining all stats
     */
    @Transactional(readOnly = true)
    public AdminStatisticsOverviewResponse getOverviewStatistics(TimePeriod period, int leaderboardLimit) {
        log.info("Calculating overview statistics for period: {}", period);

        BasicStatisticsResponse basicStats = getBasicStatistics(period);
        com.taitrinh.online_auction.dto.admin.RevenueStatisticsResponse totalRevenue = getRevenueStatistics();
        List<CategoryRevenueResponse> categoryRevenue = getRevenueByCategory();
        PendingPaymentsResponse pendingPayments = getPendingPayments();
        List<LeaderboardEntryResponse> topBidders = getTopBidders(leaderboardLimit);
        List<LeaderboardEntryResponse> topSellers = getTopSellers(leaderboardLimit);

        return AdminStatisticsOverviewResponse.builder()
                .timePeriod(period.name())
                .basicStatistics(basicStats)
                .totalRevenue(totalRevenue)
                .categoryRevenue(categoryRevenue)
                .pendingPayments(pendingPayments)
                .topBidders(topBidders)
                .topSellers(topSellers)
                .build();
    }

    // ========== HELPER METHODS ==========

    /**
     * Convert TimePeriod enum to ZonedDateTime timestamp
     */
    private ZonedDateTime getTimestampForPeriod(TimePeriod period) {
        ZonedDateTime now = ZonedDateTime.now();
        switch (period) {
            case LAST_7_DAYS:
                return now.minusDays(7);
            case LAST_30_DAYS:
                return now.minusDays(30);
            case LAST_YEAR:
                return now.minusYears(1);
            case ALL_TIME:
            default:
                return ZonedDateTime.of(2000, 1, 1, 0, 0, 0, 0, now.getZone());
        }
    }
}
