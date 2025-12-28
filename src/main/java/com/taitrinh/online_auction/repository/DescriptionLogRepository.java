package com.taitrinh.online_auction.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.taitrinh.online_auction.entity.DescriptionLog;

@Repository
public interface DescriptionLogRepository extends JpaRepository<DescriptionLog, Long> {

    // Find all description logs for a product, ordered by created_at DESC
    @Query("SELECT d FROM DescriptionLog d WHERE d.product.id = :productId ORDER BY d.createdAt DESC")
    List<DescriptionLog> findByProductIdOrderByCreatedAtDesc(@Param("productId") Long productId);

    // Count description logs for a product
    @Query("SELECT COUNT(d) FROM DescriptionLog d WHERE d.product.id = :productId")
    Long countByProductId(@Param("productId") Long productId);
}
