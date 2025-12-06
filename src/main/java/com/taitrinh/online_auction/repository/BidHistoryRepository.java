package com.taitrinh.online_auction.repository;

import java.util.List;

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
}
