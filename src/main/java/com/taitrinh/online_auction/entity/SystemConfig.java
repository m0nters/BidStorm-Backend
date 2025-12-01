package com.taitrinh.online_auction.entity;

import java.time.ZonedDateTime;

import org.hibernate.annotations.UpdateTimestamp;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "system_configs")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SystemConfig {

    @Id
    @Column(length = 100)
    private String key;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String value;

    @Column(columnDefinition = "TEXT")
    private String description;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private ZonedDateTime updatedAt;

    // Config key constants
    public static final String AUTO_EXTEND_TRIGGER_MIN = "auto_extend_trigger_min";
    public static final String AUTO_EXTEND_BY_MIN = "auto_extend_by_min";
    public static final String NEW_PRODUCT_HIGHLIGHT_MIN = "new_product_highlight_min";
    public static final String ALLOW_UNRATED_BIDDERS = "allow_unrated_bidders";
    public static final String SELLER_TEMP_DURATION_DAYS = "seller_temp_duration_days";

    // Helper methods
    public Integer getIntValue() {
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    public Boolean getBooleanValue() {
        return Boolean.parseBoolean(value);
    }

    public Long getLongValue() {
        try {
            return Long.parseLong(value);
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
