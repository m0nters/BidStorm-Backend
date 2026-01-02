package com.taitrinh.online_auction.repository;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.taitrinh.online_auction.entity.Product;

@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {

        // Top 5 products ending soon (not ended, sorted by end_time ascending)
        @Query("SELECT p FROM Product p " +
                        "LEFT JOIN FETCH p.category " +
                        "WHERE p.isEnded = false ORDER BY p.endTime ASC")
        List<Product> findTop5EndingSoon(Pageable pageable);

        // Top 5 products with most bids (sorted by bid_count descending)
        @Query("SELECT p FROM Product p " +
                        "LEFT JOIN FETCH p.category " +
                        "WHERE p.isEnded = false ORDER BY p.bidCount DESC")
        List<Product> findTop5ByBidCount(Pageable pageable);

        // Top 5 products with highest price (sorted by current_price descending)
        @Query("SELECT p FROM Product p " +
                        "LEFT JOIN FETCH p.category " +
                        "WHERE p.isEnded = false ORDER BY p.currentPrice DESC")
        List<Product> findTop5ByPrice(Pageable pageable);

        // Find products by category with pagination
        @Query("SELECT p FROM Product p WHERE p.category.id = :categoryId AND p.isEnded = false")
        Page<Product> findByCategoryId(@Param("categoryId") Integer categoryId, Pageable pageable);

        // Find products by category or its children with pagination
        @Query("SELECT p FROM Product p WHERE (p.category.id = :categoryId OR p.category.parent.id = :categoryId) AND p.isEnded = false")
        Page<Product> findByCategoryIdOrParentId(@Param("categoryId") Integer categoryId, Pageable pageable);

        // Full-text search using PostgreSQL tsvector (supports Vietnamese without
        // diacritics)
        @Query(value = "SELECT p.* FROM products p " +
                        "WHERE p.search_vector @@ plainto_tsquery('simple', unaccent(:keyword)) " +
                        "AND p.is_ended = false", nativeQuery = true, countQuery = "SELECT COUNT(*) FROM products p "
                                        +
                                        "WHERE p.search_vector @@ plainto_tsquery('simple', unaccent(:keyword)) " +
                                        "AND p.is_ended = false")
        Page<Product> searchByTitle(@Param("keyword") String keyword, Pageable pageable);

        // Full-text search by title and category using PostgreSQL tsvector
        @Query(value = "SELECT p.* FROM products p " +
                        "INNER JOIN categories c ON p.category_id = c.id " +
                        "WHERE p.search_vector @@ plainto_tsquery('simple', unaccent(:keyword)) " +
                        "AND (p.category_id = :categoryId OR c.parent_id = :categoryId) " +
                        "AND p.is_ended = false", nativeQuery = true, countQuery = "SELECT COUNT(*) FROM products p "
                                        +
                                        "INNER JOIN categories c ON p.category_id = c.id " +
                                        "WHERE p.search_vector @@ plainto_tsquery('simple', unaccent(:keyword)) " +
                                        "AND (p.category_id = :categoryId OR c.parent_id = :categoryId) " +
                                        "AND p.is_ended = false")
        Page<Product> searchByTitleAndCategory(@Param("keyword") String keyword,
                        @Param("categoryId") Integer categoryId,
                        Pageable pageable);

        // Find product by id with all related data eagerly loaded
        @Query("SELECT p FROM Product p " +
                        "LEFT JOIN FETCH p.seller " +
                        "LEFT JOIN FETCH p.category c " +
                        "LEFT JOIN FETCH c.parent " +
                        "LEFT JOIN FETCH p.highestBidder " +
                        "LEFT JOIN FETCH p.images " +
                        "WHERE p.id = :id AND p.isEnded = false")
        Optional<Product> findByIdWithDetails(@Param("id") Long id);

        // Find 5 other products in same category (excluding current product)
        @Query("SELECT p FROM Product p WHERE p.category.id = :categoryId AND p.id != :excludeId AND p.isEnded = false ORDER BY p.createdAt DESC")
        List<Product> findRelatedProducts(@Param("categoryId") Integer categoryId, @Param("excludeId") Long excludeId,
                        Pageable pageable);

        // Count active products by category
        @Query("SELECT COUNT(p) FROM Product p WHERE p.category.id = :categoryId AND p.isEnded = false")
        long countByCategoryId(@Param("categoryId") Integer categoryId);

        // Slug-related methods
        @Query("SELECT p FROM Product p " +
                        "LEFT JOIN FETCH p.seller " +
                        "LEFT JOIN FETCH p.category c " +
                        "LEFT JOIN FETCH c.parent " +
                        "LEFT JOIN FETCH p.highestBidder " +
                        "LEFT JOIN FETCH p.images " +
                        "WHERE p.slug = :slug")
        Optional<Product> findBySlug(@Param("slug") String slug);

        boolean existsBySlug(String slug);

        // Find products won by a user (where user is winner and product ended)
        @Query("SELECT p FROM Product p " +
                        "LEFT JOIN FETCH p.seller " +
                        "LEFT JOIN FETCH p.category c " +
                        "LEFT JOIN FETCH c.parent " +
                        "LEFT JOIN FETCH p.images " +
                        "WHERE p.winner.id = :userId AND p.isEnded = true "
                        +
                        "ORDER BY p.endTime DESC")
        Page<Product> findByWinner_IdAndIsEndedTrue(@Param("userId") Long userId, Pageable pageable);

        // Find products by a list of category IDs with pagination (for parent category
        // queries)
        @Query("SELECT p FROM Product p WHERE p.category.id IN :categoryIds AND p.isEnded = false")
        Page<Product> findByCategoryIdIn(@Param("categoryIds") List<Integer> categoryIds, Pageable pageable);

        // Find products that ended between two timestamps (for cron job processing)
        @Query("SELECT p FROM Product p WHERE p.endTime > :startTime AND p.endTime <= :endTime AND p.isEnded = false")
        List<Product> findProductsEndingBetween(@Param("startTime") ZonedDateTime startTime,
                        @Param("endTime") ZonedDateTime endTime);

        // Find seller's active products (not ended)
        Page<Product> findBySeller_IdAndIsEndedFalseOrderByCreatedAtDesc(Long sellerId, Pageable pageable);

        // Find seller's ended products with winners
        @Query("SELECT p FROM Product p " +
                        "LEFT JOIN FETCH p.winner " +
                        "LEFT JOIN FETCH p.images " +
                        "WHERE p.seller.id = :sellerId AND p.isEnded = true AND p.winner IS NOT NULL " +
                        "ORDER BY p.endTime DESC")
        Page<Product> findBySeller_IdAndIsEndedTrueAndWinnerIsNotNull(@Param("sellerId") Long sellerId,
                        Pageable pageable);

        // Find products that ended between two timestamps (for cron job processing)
        @Query("SELECT p FROM Product p " +
                        "LEFT JOIN FETCH p.seller " +
                        "LEFT JOIN FETCH p.highestBidder " +
                        "WHERE p.isEnded = false AND p.endTime BETWEEN :startTime AND :endTime")
        List<Product> findByIsEndedFalseAndEndTimeBetween(
                        @Param("startTime") java.time.ZonedDateTime startTime,
                        @Param("endTime") java.time.ZonedDateTime endTime);
}
