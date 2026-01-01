package com.taitrinh.online_auction.dto.order;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ConfirmShipmentRequest {

    @NotBlank(message = "Mã vận đơn không được để trống")
    @Size(max = 100, message = "Mã vận đơn không được quá 100 ký tự")
    private String trackingNumber;
}
