package com.taitrinh.online_auction.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.taitrinh.online_auction.entity.Favorite;

@Repository
public interface FavoriteRepository extends JpaRepository<Favorite, Favorite.FavoriteId> {

    // Find all favorites for a user with pagination
    @Query("SELECT f FROM Favorite f " +
            "LEFT JOIN FETCH f.product p " +
            "LEFT JOIN FETCH p.category c " +
            "LEFT JOIN FETCH c.parent " +
            "WHERE f.user.id = :userId " +
            "ORDER BY f.createdAt DESC")
    Page<Favorite> findByUser_IdOrderByCreatedAtDesc(@Param("userId") Long userId, Pageable pageable);

    // Find all favorites for a user (non-paginated)
    List<Favorite> findByUser_IdOrderByCreatedAtDesc(Long userId);

    // Check if a product is favorited by user
    boolean existsByUser_IdAndProduct_Id(Long userId, Long productId);

    // Find specific favorite
    Optional<Favorite> findByUser_IdAndProduct_Id(Long userId, Long productId);

    // Delete favorite by user and product
    void deleteByUser_IdAndProduct_Id(Long userId, Long productId);

    // Delete all favorites for a product (used when deleting product)
    int deleteByProduct_Id(Long productId);

    // Count favorites for a user
    long countByUser_Id(Long userId);
}
