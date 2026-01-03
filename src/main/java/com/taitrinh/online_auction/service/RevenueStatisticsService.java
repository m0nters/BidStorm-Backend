package com.taitrinh.online_auction.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.taitrinh.online_auction.dto.admin.RevenueStatisticsResponse;
import com.taitrinh.online_auction.entity.OrderCompletion;
import com.taitrinh.online_auction.entity.OrderCompletion.OrderStatus;
import com.taitrinh.online_auction.repository.OrderCompletionRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class RevenueStatisticsService {

    private final OrderCompletionRepository orderCompletionRepository;

    /**
     * Get revenue statistics from completed orders
     */
    @Transactional(readOnly = true)
    public RevenueStatisticsResponse getRevenueStatistics() {
        List<OrderCompletion> completedOrders = orderCompletionRepository.findAllByStatus(OrderStatus.COMPLETED);

        long totalRevenue = completedOrders.stream()
                .mapToLong(OrderCompletion::getAmountCents)
                .sum();

        long orderCount = completedOrders.size();
        long averageOrderValue = orderCount > 0 ? totalRevenue / orderCount : 0;

        // Get currency from first order, or default to VND
        String currency = completedOrders.isEmpty() ? "VND" : completedOrders.get(0).getCurrency();

        log.info("Revenue statistics calculated: total={} cents, orders={}, avg={} cents",
                totalRevenue, orderCount, averageOrderValue);

        return RevenueStatisticsResponse.builder()
                .totalRevenueCents(totalRevenue)
                .completedOrderCount(orderCount)
                .currency(currency)
                .averageOrderValueCents(averageOrderValue)
                .build();
    }
}
