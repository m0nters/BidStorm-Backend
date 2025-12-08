package com.taitrinh.online_auction.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.taitrinh.online_auction.dto.ApiResponse;
import com.taitrinh.online_auction.service.ConfigService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/config")
@RequiredArgsConstructor
@Tag(name = "Config", description = "APIs for managing configs")
public class ConfigCotroller {

    private final ConfigService configService;

    @GetMapping("/auto-extend-trigger-min")
    @Operation(summary = "Get auto extend trigger min", description = "Get the number of minutes before auction end time to trigger auto-extension")
    public ResponseEntity<ApiResponse<Integer>> getAutoExtendTriggerMin() {
        Integer autoExtendTriggerMin = configService.getAutoExtendTriggerMin();
        return ResponseEntity
                .ok(ApiResponse.ok(autoExtendTriggerMin, "Auto extend trigger min retrieved successfully"));
    }

    @GetMapping("/auto-extend-by-min")
    @Operation(summary = "Get auto extend by min", description = "Get the number of minutes to extend auction duration when auto-extension is triggered")
    public ResponseEntity<ApiResponse<Integer>> getAutoExtendByMin() {
        Integer autoExtendByMin = configService.getAutoExtendByMin();
        return ResponseEntity.ok(ApiResponse.ok(autoExtendByMin, "Auto extend by min retrieved successfully"));
    }
}
