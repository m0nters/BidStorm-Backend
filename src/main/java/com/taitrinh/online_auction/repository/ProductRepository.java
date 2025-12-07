package com.taitrinh.online_auction.repository;

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
        @Query("SELECT p FROM Product p WHERE p.endTime > CURRENT_TIMESTAMP ORDER BY p.endTime ASC")
        List<Product> findTop5EndingSoon(Pageable pageable);

        // Top 5 products with most bids (sorted by bid_count descending)
        @Query("SELECT p FROM Product p WHERE p.isEnded = false ORDER BY p.bidCount DESC")
        List<Product> findTop5ByBidCount(Pageable pageable);

        // Top 5 products with highest price (sorted by current_price descending)
        @Query("SELECT p FROM Product p WHERE p.isEnded = false ORDER BY p.currentPrice DESC")
        List<Product> findTop5ByPrice(Pageable pageable);

        // Find products by category with pagination
        @Query("SELECT p FROM Product p WHERE p.category.id = :categoryId AND p.isEnded = false")
        Page<Product> findByCategoryId(@Param("categoryId") Integer categoryId, Pageable pageable);

        // Find products by category or its children with pagination
        @Query("SELECT p FROM Product p WHERE (p.category.id = :categoryId OR p.category.parent.id = :categoryId) AND p.isEnded = false")
        Page<Product> findByCategoryIdOrParentId(@Param("categoryId") Integer categoryId, Pageable pageable);

        // Full-text search by title (case-insensitive, supports Vietnamese without
        // diacritics)
        @Query("SELECT p FROM Product p WHERE LOWER(FUNCTION('unaccent', p.title)) LIKE LOWER(FUNCTION('unaccent', CONCAT('%', :keyword, '%'))) AND p.isEnded = false")
        Page<Product> searchByTitle(@Param("keyword") String keyword, Pageable pageable);

        // Full-text search by title and category
        @Query("SELECT p FROM Product p WHERE LOWER(FUNCTION('unaccent', p.title)) LIKE LOWER(FUNCTION('unaccent', CONCAT('%', :keyword, '%'))) "
                        +
                        "AND (p.category.id = :categoryId OR p.category.parent.id = :categoryId) AND p.isEnded = false")
        Page<Product> searchByTitleAndCategory(@Param("keyword") String keyword,
                        @Param("categoryId") Integer categoryId,
                        Pageable pageable);

        // Find product by id with all related data eagerly loaded
        @Query("SELECT p FROM Product p " +
                        "LEFT JOIN FETCH p.seller " +
                        "LEFT JOIN FETCH p.category " +
                        "LEFT JOIN FETCH p.highestBidder " +
                        "LEFT JOIN FETCH p.images " +
                        "WHERE p.id = :id")
        Optional<Product> findByIdWithDetails(@Param("id") Long id);

        // Find 5 other products in same category (excluding current product)
        @Query("SELECT p FROM Product p WHERE p.category.id = :categoryId AND p.id != :excludeId AND p.isEnded = false ORDER BY p.createdAt DESC")
        List<Product> findRelatedProducts(@Param("categoryId") Integer categoryId, @Param("excludeId") Long excludeId,
                        Pageable pageable);

        // Count active products by category
        @Query("SELECT COUNT(p) FROM Product p WHERE p.category.id = :categoryId AND p.isEnded = false")
        long countByCategoryId(@Param("categoryId") Integer categoryId);

        // Slug-related methods
        Optional<Product> findBySlug(String slug);

        boolean existsBySlug(String slug);
}
