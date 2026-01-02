package com.taitrinh.online_auction.repository;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.taitrinh.online_auction.entity.Review;

@Repository
public interface ReviewRepository extends JpaRepository<Review, Long> {

        // Find all reviews for a user (as reviewee) with pagination
        @Query("SELECT r FROM Review r " +
                        "LEFT JOIN FETCH r.reviewer " +
                        "LEFT JOIN FETCH r.product " +
                        "WHERE r.reviewee.id = :revieweeId " +
                        "ORDER BY r.createdAt DESC")
        Page<Review> findByReviewee_IdOrderByCreatedAtDesc(@Param("revieweeId") Long revieweeId, Pageable pageable);

        // Find all reviews made by a user (as reviewer) with pagination
        @Query("SELECT r FROM Review r " +
                        "LEFT JOIN FETCH r.reviewee " +
                        "LEFT JOIN FETCH r.product " +
                        "WHERE r.reviewer.id = :reviewerId " +
                        "ORDER BY r.createdAt DESC")
        Page<Review> findByReviewer_IdOrderByCreatedAtDesc(@Param("reviewerId") Long reviewerId, Pageable pageable);

        // Check if review exists for a product by a reviewer
        boolean existsByProduct_IdAndReviewer_Id(Long productId, Long reviewerId);

        // Find specific review by product and reviewer
        @Query("SELECT r FROM Review r " +
                        "LEFT JOIN FETCH r.reviewer " +
                        "LEFT JOIN FETCH r.reviewee " +
                        "LEFT JOIN FETCH r.product " +
                        "WHERE r.product.id = :productId AND r.reviewer.id = :reviewerId")
        Optional<Review> findByProduct_IdAndReviewer_Id(@Param("productId") Long productId,
                        @Param("reviewerId") Long reviewerId);

        // Count reviews for a user (as reviewee)
        long countByReviewee_Id(Long revieweeId);

        // Count positive reviews for a user
        @Query("SELECT COUNT(r) FROM Review r WHERE r.reviewee.id = :revieweeId AND r.rating = 1")
        long countPositiveReviewsByReviewee_Id(@Param("revieweeId") Long revieweeId);

        // Count negative reviews for a user
        @Query("SELECT COUNT(r) FROM Review r WHERE r.reviewee.id = :revieweeId AND r.rating = -1")
        long countNegativeReviewsByReviewee_Id(@Param("revieweeId") Long revieweeId);
}
