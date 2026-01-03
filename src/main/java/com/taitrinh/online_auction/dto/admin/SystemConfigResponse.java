package com.taitrinh.online_auction.dto.admin;

import java.time.ZonedDateTime;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "System configuration entry")
public class SystemConfigResponse {

    @Schema(description = "Configuration key", example = "auto_extend_trigger_min")
    private String key;

    @Schema(description = "Configuration value", example = "5")
    private String value;

    @Schema(description = "Configuration description", example = "Minutes before auction end to trigger auto-extension")
    private String description;

    @Schema(description = "Last update timestamp")
    private ZonedDateTime updatedAt;
}
