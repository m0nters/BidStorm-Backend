package com.taitrinh.online_auction.mapper;

import org.springframework.stereotype.Component;

import com.taitrinh.online_auction.dto.admin.SystemConfigResponse;
import com.taitrinh.online_auction.entity.SystemConfig;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class SystemConfigMapper {

    /**
     * Map SystemConfig entity to SystemConfigResponse DTO
     */
    public SystemConfigResponse toSystemConfigResponse(SystemConfig config) {
        return SystemConfigResponse.builder()
                .key(config.getKey())
                .value(config.getValue())
                .description(config.getDescription())
                .updatedAt(config.getUpdatedAt())
                .build();
    }
}
