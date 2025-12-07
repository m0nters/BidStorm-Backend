package com.taitrinh.online_auction.service;

import org.springframework.stereotype.Service;

import com.taitrinh.online_auction.entity.SystemConfig;
import com.taitrinh.online_auction.repository.SystemConfigRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Centralized service for accessing database-backed system configuration
 * values.
 * These configs are admin-manageable website settings stored in the
 * system_configs table.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ConfigService {

    private final SystemConfigRepository systemConfigRepository;

    // Default values for system configs for fallback
    private static final Integer DEFAULT_NEW_PRODUCT_HIGHLIGHT_MIN = 60;
    private static final Integer DEFAULT_AUTO_EXTEND_TRIGGER_MIN = 5;
    private static final Integer DEFAULT_AUTO_EXTEND_BY_MIN = 10;
    private static final Integer DEFAULT_SELLER_TEMP_DURATION_DAYS = 7;

    /**
     * Get the number of minutes a product should be highlighted as "new"
     * 
     * @return Minutes to highlight new products (default: 60)
     */
    public Integer getNewProductHighlightMin() {
        return getIntConfig(
                SystemConfig.NEW_PRODUCT_HIGHLIGHT_MIN,
                DEFAULT_NEW_PRODUCT_HIGHLIGHT_MIN);
    }

    /**
     * Get the number of minutes before auction end time to trigger auto-extension
     * 
     * @return Minutes before end to trigger auto-extend (default: 5)
     */
    public Integer getAutoExtendTriggerMin() {
        return getIntConfig(
                SystemConfig.AUTO_EXTEND_TRIGGER_MIN,
                DEFAULT_AUTO_EXTEND_TRIGGER_MIN);
    }

    /**
     * Get the number of minutes to extend auction by when auto-extend is triggered
     * 
     * @return Minutes to extend auction (default: 10)
     */
    public Integer getAutoExtendByMin() {
        return getIntConfig(
                SystemConfig.AUTO_EXTEND_BY_MIN,
                DEFAULT_AUTO_EXTEND_BY_MIN);
    }

    /**
     * Get the number of days a temporary seller permission lasts
     * 
     * @return Days of temporary seller permission (default: 7)
     */
    public Integer getSellerTempDurationDays() {
        return getIntConfig(
                SystemConfig.SELLER_TEMP_DURATION_DAYS,
                DEFAULT_SELLER_TEMP_DURATION_DAYS);
    }

    /**
     * Generic method to retrieve integer config with fallback
     */
    private Integer getIntConfig(String key, Integer defaultValue) {
        try {
            return systemConfigRepository.findByKey(key)
                    .map(SystemConfig::getIntValue)
                    .orElse(defaultValue);
        } catch (Exception e) {
            log.warn("Error getting config '{}', using default: {}", key, defaultValue, e);
            return defaultValue;
        }
    }
}
