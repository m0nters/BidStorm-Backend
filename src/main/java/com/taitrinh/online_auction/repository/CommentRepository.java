package com.taitrinh.online_auction.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.taitrinh.online_auction.entity.Comment;

@Repository
public interface CommentRepository extends JpaRepository<Comment, Long> {

    /**
     * Find all top-level comments (questions) for a product
     * Ordered by newest first
     */
    @Query("SELECT c FROM Comment c WHERE c.product.id = :productId AND c.parent IS NULL ORDER BY c.createdAt DESC")
    List<Comment> findTopLevelCommentsByProductId(@Param("productId") Long productId);

    /**
     * Find all replies to a specific comment
     */
    @Query("SELECT c FROM Comment c WHERE c.parent.id = :parentId ORDER BY c.createdAt DESC")
    List<Comment> findRepliesByParentId(@Param("parentId") Long parentId);

    /**
     * Find all comments (questions and replies) for a product
     */
    @Query("SELECT c FROM Comment c LEFT JOIN FETCH c.user WHERE c.product.id = :productId ORDER BY c.createdAt DESC")
    List<Comment> findAllByProductIdWithUser(@Param("productId") Long productId);

    /**
     * Count top-level comments (questions) for a product
     */
    @Query("SELECT COUNT(c) FROM Comment c WHERE c.product.id = :productId AND c.parent IS NULL")
    Long countQuestionsByProductId(@Param("productId") Long productId);

    /**
     * Find all distinct users who have asked questions on a product (for email
     * notifications)
     */
    @Query("SELECT DISTINCT c.user FROM Comment c WHERE c.product.id = :productId AND c.parent IS NULL")
    List<com.taitrinh.online_auction.entity.User> findDistinctQuestionAskersByProductId(
            @Param("productId") Long productId);
}
