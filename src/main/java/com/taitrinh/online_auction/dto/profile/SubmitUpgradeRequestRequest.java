package com.taitrinh.online_auction.dto.profile;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Request to submit upgrade request from BIDDER to SELLER")
public class SubmitUpgradeRequestRequest {

    @Size(max = 1000, message = "Lý do không được vượt quá 1000 ký tự")
    @Schema(description = "Reason for requesting upgrade to seller role", example = "Tôi muốn bán các sản phẩm điện tử cũ của mình")
    private String reason;
}
