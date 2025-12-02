package com.taitrinh.online_auction.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.taitrinh.online_auction.entity.Category;

@Repository
public interface CategoryRepository extends JpaRepository<Category, Integer> {

    // Find all top-level categories (parent = null)
    List<Category> findByParentIsNull();

    // Find all sub-categories of a parent
    List<Category> findByParentId(Integer parentId);

    // Check if category exists by name and parent
    boolean existsByNameAndParentId(String name, Integer parentId);

    // Check if category exists by name (for top-level)
    boolean existsByNameAndParentIsNull(String name);

    // Find category with children loaded
    @Query("SELECT c FROM Category c LEFT JOIN FETCH c.children WHERE c.id = :id")
    Optional<Category> findByIdWithChildren(@Param("id") Integer id);

    // Check if category has products
    @Query("SELECT COUNT(p) > 0 FROM Product p WHERE p.category.id = :categoryId")
    boolean hasProducts(@Param("categoryId") Integer categoryId);
}
