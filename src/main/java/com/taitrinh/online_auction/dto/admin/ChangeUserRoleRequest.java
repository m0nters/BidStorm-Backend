package com.taitrinh.online_auction.dto.admin;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Request to change user's role")
public class ChangeUserRoleRequest {

    @NotNull(message = "Role ID không được để trống")
    @Min(value = 1, message = "Role ID phải từ 1 đến 3")
    @Max(value = 3, message = "Role ID phải từ 1 đến 3")
    @Schema(description = "New role ID (1=ADMIN, 2=SELLER, 3=BIDDER)", example = "2")
    private Short roleId;
}
