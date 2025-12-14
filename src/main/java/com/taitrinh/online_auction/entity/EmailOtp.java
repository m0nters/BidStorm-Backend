package com.taitrinh.online_auction.entity;

import java.time.ZonedDateTime;

import org.hibernate.annotations.CreationTimestamp;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "email_otps", indexes = {
        @Index(name = "idx_email_otps_lookup", columnList = "email, otp_code, purpose, is_used")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EmailOtp {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String email;

    @Column(name = "otp_code", nullable = false, length = 6)
    private String otpCode;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private OtpPurpose purpose;

    @Column(name = "is_used", nullable = false)
    @Builder.Default
    private Boolean isUsed = false;

    @Column(name = "expires_at", nullable = false)
    private ZonedDateTime expiresAt;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private ZonedDateTime createdAt;

    public enum OtpPurpose {
        EMAIL_VERIFICATION, PASSWORD_RESET
    }

    // Helper methods
    public boolean isExpired() {
        return expiresAt != null && expiresAt.isBefore(ZonedDateTime.now());
    }

    public boolean isValid() {
        return !isUsed && !isExpired();
    }
}
