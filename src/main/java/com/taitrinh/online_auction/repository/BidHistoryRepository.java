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
import com.taitrinh.online_auction.entity.User;

@Repository
public interface BidHistoryRepository extends JpaRepository<BidHistory, Long> {

        // Get bid history for a product, ordered by bid time descending (newest first)
        @Query("SELECT b FROM BidHistory b WHERE b.product.id = :productId ORDER BY b.createdAt DESC")
        List<BidHistory> findByProductIdOrderByCreatedAtDesc(@Param("productId") Long productId);

        // Find bid history for a bidder where product is not ended
        @Query("SELECT b FROM BidHistory b " +
                        "WHERE b.bidder.id = :bidderId AND b.product.isEnded = false " +
                        "ORDER BY b.createdAt DESC")
        Page<BidHistory> findByBidder_IdAndProduct_IsEndedFalse(@Param("bidderId") Long bidderId, Pageable pageable);

        // Get current highest max bid for a product (for automatic bidding logic)
        Optional<BidHistory> findFirstByProductIdOrderByMaxBidAmountDescCreatedAtAsc(
                        @Param("productId") Long productId);

        // Get all bids from a specific bidder for a product
        @Query("SELECT b FROM BidHistory b " +
                        "WHERE b.product.id = :productId AND b.bidder.id = :bidderId " +
                        "ORDER BY b.createdAt DESC")
        List<BidHistory> findByProductIdAndBidderId(
                        @Param("productId") Long productId,
                        @Param("bidderId") Long bidderId);

        // Find user's highest max bid for a specific product
        @Query("SELECT b FROM BidHistory b " +
                        "WHERE b.product.id = :productId AND b.bidder.id = :bidderId " +
                        "ORDER BY b.maxBidAmount DESC, b.createdAt ASC")
        Optional<BidHistory> findTopByProductIdAndBidderIdOrderByMaxBidAmountDesc(
                        @Param("productId") Long productId,
                        @Param("bidderId") Long bidderId);

        // Get all distinct bidders who have bid on a product (for email notifications)
        @Query("SELECT DISTINCT b.bidder FROM BidHistory b WHERE b.product.id = :productId")
        List<User> findDistinctBiddersByProductId(
                        @Param("productId") Long productId);

        // Get winning bid (highest bid amount) for a product
        @Query("SELECT b FROM BidHistory b " +
                        "LEFT JOIN FETCH b.bidder " +
                        "WHERE b.product.id = :productId " +
                        "ORDER BY b.bidAmount DESC")
        java.util.Optional<BidHistory> findTopByProductIdOrderByBidAmountDesc(@Param("productId") Long productId);
}
