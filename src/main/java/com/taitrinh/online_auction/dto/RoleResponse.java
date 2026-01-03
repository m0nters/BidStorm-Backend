package com.taitrinh.online_auction.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Role information")
public class RoleResponse {

    @Schema(description = "Role ID (1=ADMIN, 2=SELLER, 3=BIDDER)", example = "3")
    private Short id;

    @Schema(description = "Role name", example = "BIDDER")
    private String name;
}
