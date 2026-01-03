package com.taitrinh.online_auction.entity;

import java.time.LocalDate;
import java.time.ZonedDateTime;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "users", indexes = {
        @Index(name = "idx_users_email", columnList = "email"),
        @Index(name = "idx_users_role", columnList = "role_id"),
        @Index(name = "idx_users_rating", columnList = "positive_rating, negative_rating")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(name = "password_hash", nullable = false)
    private String passwordHash;

    @Column(name = "full_name", nullable = false)
    private String fullName;

    @Column(columnDefinition = "TEXT")
    private String address;

    @Column(name = "birth_date")
    private LocalDate birthDate;

    @Column(name = "avatar_url", columnDefinition = "TEXT")
    @Builder.Default
    private String avatarUrl = "https://bidstorm.s3.ap-southeast-2.amazonaws.com/avatar.png";

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "role_id", nullable = false)
    private Role role;

    @Column(name = "seller_expires_at")
    private ZonedDateTime sellerExpiresAt;

    @ManyToOne
    @JoinColumn(name = "seller_upgraded_by")
    private User sellerUpgradedBy;

    @Column(name = "positive_rating", nullable = false)
    @Builder.Default
    private Integer positiveRating = 0;

    @Column(name = "negative_rating", nullable = false)
    @Builder.Default
    private Integer negativeRating = 0;

    @Column(name = "email_verified", nullable = false)
    @Builder.Default
    private Boolean emailVerified = false;

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private Boolean isActive = true;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private ZonedDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private ZonedDateTime updatedAt;

    // Helper methods
    public boolean isSeller() {
        // ADMIN can also sell (role hierarchy: ADMIN > SELLER)
        return role != null && (role.getId() == Role.SELLER || role.getId() == Role.ADMIN);
    }

    public boolean isAdmin() {
        return role != null && role.getId() == Role.ADMIN;
    }

    public double getRatingPercentage() {
        int total = positiveRating + negativeRating;
        return total == 0 ? 0.0 : (positiveRating * 100.0) / total;
    }

    public boolean canBid() {
        return getRatingPercentage() >= 80.0;
    }
}
