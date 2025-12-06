package com.taitrinh.online_auction.repository;

import java.time.ZonedDateTime;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.taitrinh.online_auction.entity.EmailOtp;
import com.taitrinh.online_auction.entity.EmailOtp.OtpPurpose;

@Repository
public interface EmailOtpRepository extends JpaRepository<EmailOtp, Long> {

    @Query("SELECT o FROM EmailOtp o WHERE o.email = :email AND o.otpCode = :otpCode " +
            "AND o.purpose = :purpose AND o.isUsed = false AND o.expiresAt > :now")
    Optional<EmailOtp> findValidOtp(
            @Param("email") String email,
            @Param("otpCode") String otpCode,
            @Param("purpose") OtpPurpose purpose,
            @Param("now") ZonedDateTime now);

    @Modifying
    @Query("UPDATE EmailOtp o SET o.isUsed = true WHERE o.email = :email AND o.purpose = :purpose AND o.isUsed = false")
    void markAllAsUsed(@Param("email") String email, @Param("purpose") OtpPurpose purpose);

    @Modifying
    @Query("DELETE FROM EmailOtp o WHERE o.expiresAt < :now")
    void deleteExpired(@Param("now") ZonedDateTime now);
}
