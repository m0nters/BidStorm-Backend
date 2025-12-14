package com.taitrinh.online_auction.repository;

import java.time.ZonedDateTime;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.taitrinh.online_auction.entity.RefreshToken;

@Repository
public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {

    Optional<RefreshToken> findByToken(String token);

    Optional<RefreshToken> findByUserId(Long userId);

    /**
     * Find a valid (non-revoked) refresh token by its token string.
     * Used for token validation during refresh operations.
     */
    @Query("SELECT r FROM RefreshToken r WHERE r.token = :token AND r.revokedAt IS NULL")
    Optional<RefreshToken> findValidByToken(@Param("token") String token);

    /**
     * Find a refresh token by the new token that replaced it.
     * Used for detecting token reuse attacks.
     */
    @Query("SELECT r FROM RefreshToken r WHERE r.replacedBy = :newToken")
    Optional<RefreshToken> findByReplacedBy(@Param("newToken") String newToken);

    /**
     * Revoke all refresh tokens for a specific user.
     * Used for security breach detection (when a revoked token is reused)
     * and during logout to maintain audit trail.
     */
    @Modifying
    @Query("UPDATE RefreshToken r SET r.revokedAt = :revokedAt WHERE r.user.id = :userId AND r.revokedAt IS NULL")
    void revokeAllByUserId(@Param("userId") Long userId, @Param("revokedAt") ZonedDateTime revokedAt);

    /**
     * Delete expired tokens (cleanup job).
     * Note: We keep revoked tokens for audit trail, only delete truly expired ones.
     */
    @Modifying
    @Query("DELETE FROM RefreshToken r WHERE r.expiresAt < :now")
    void deleteExpired(@Param("now") ZonedDateTime now);

    /**
     * Delete all tokens for a user (deprecated - use revokeAllByUserId instead).
     * Kept for backward compatibility but should be phased out.
     */
    @Modifying
    void deleteByUserId(@Param("userId") Long userId);
}
