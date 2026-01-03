package com.taitrinh.online_auction.repository;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.taitrinh.online_auction.entity.User;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    // Find user by email
    Optional<User> findByEmail(String email);

    // Check if email exists
    boolean existsByEmail(String email);

    // Find user by email (with role loaded)
    @Query("SELECT u FROM User u LEFT JOIN FETCH u.role WHERE u.email = :email")
    Optional<User> findByEmailWithRole(@Param("email") String email);

    // Check if user is active
    @Query("SELECT u FROM User u WHERE u.id = :id AND u.isActive = true")
    Optional<User> findActiveUserById(@Param("id") Long id);

    // Admin user management - pagination and filtering
    Page<User> findAllByRole_Id(Short roleId, Pageable pageable);

    Page<User> findAllByIsActive(Boolean isActive, Pageable pageable);

    Page<User> findAllByRole_IdAndIsActive(Short roleId, Boolean isActive, Pageable pageable);
}
