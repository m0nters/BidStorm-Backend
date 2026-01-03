package com.taitrinh.online_auction.repository;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.taitrinh.online_auction.entity.UpgradeRequest;
import com.taitrinh.online_auction.entity.UpgradeRequest.UpgradeStatus;

@Repository
public interface UpgradeRequestRepository extends JpaRepository<UpgradeRequest, Long> {

    List<UpgradeRequest> findAllByStatusOrderByCreatedAtDesc(UpgradeStatus status);

    List<UpgradeRequest> findAllByOrderByCreatedAtDesc();

    Optional<UpgradeRequest> findByBidder_Id(Long bidderId);

    Optional<UpgradeRequest> findByBidder_IdAndStatus(Long bidderId, UpgradeStatus status);

    Optional<UpgradeRequest> findByBidder_IdAndStatusAndCreatedAtAfter(Long bidderId, UpgradeStatus status,
            ZonedDateTime time);

    // === STATISTICS METHODS ===

    // Count requests by status
    long countByStatus(UpgradeStatus status);

    // Count approved/rejected requests after timestamp
    long countByStatusAndReviewedAtAfter(UpgradeStatus status, ZonedDateTime timestamp);
}
