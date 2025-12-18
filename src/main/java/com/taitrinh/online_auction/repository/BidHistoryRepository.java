package com.taitrinh.online_auction.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.taitrinh.online_auction.entity.BidHistory;

@Repository
public interface BidHistoryRepository extends JpaRepository<BidHistory, Long> {

        // Get bid history for a product, ordered by bid time descending
        @Query("SELECT b FROM BidHistory b WHERE b.product.id = :productId ORDER BY b.createdAt DESC")
        List<BidHistory> findByProductIdOrderByCreatedAtDesc(@Param("productId") Long productId);

        // Find bid history for a bidder where product is not ended
        @Query("SELECT b FROM BidHistory b " +
                        "WHERE b.bidder.id = :bidderId AND b.product.isEnded = false " +
                        "ORDER BY b.createdAt DESC")
        Page<BidHistory> findByBidder_IdAndProduct_IsEndedFalse(@Param("bidderId") Long bidderId, Pageable pageable);

        // Find user's highest bid for a specific product
        @Query("SELECT b FROM BidHistory b " +
                        "WHERE b.product.id = :productId AND b.bidder.id = :bidderId " +
                        "ORDER BY b.bidAmount DESC")
        Optional<BidHistory> findTopByProduct_IdAndBidder_IdOrderByBidAmountDesc(
                        @Param("productId") Long productId,
                        @Param("bidderId") Long bidderId);
}
