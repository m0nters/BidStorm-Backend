package com.taitrinh.online_auction.dto.admin;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Request to update system configuration value")
public class UpdateSystemConfigRequest {

    @NotBlank(message = "Value không được để trống")
    @Schema(description = "New configuration value", example = "10")
    private String value;
}
