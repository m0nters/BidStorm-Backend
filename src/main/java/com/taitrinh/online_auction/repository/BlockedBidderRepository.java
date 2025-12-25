package com.taitrinh.online_auction.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.taitrinh.online_auction.entity.BlockedBidder;
import com.taitrinh.online_auction.entity.BlockedBidder.BlockedBidderId;

@Repository
public interface BlockedBidderRepository extends JpaRepository<BlockedBidder, BlockedBidderId> {

    /**
     * Check if a bidder is blocked for a specific product
     */
    boolean existsByProduct_IdAndBidder_Id(Long productId, Long bidderId);

    /**
     * Find a blocked bidder record
     */
    Optional<BlockedBidder> findByProduct_IdAndBidder_Id(Long productId, Long bidderId);
}
