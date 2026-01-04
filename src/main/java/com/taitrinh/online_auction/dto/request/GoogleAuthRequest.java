package com.taitrinh.online_auction.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class GoogleAuthRequest {
    @NotBlank(message = "Mã xác thực không được để trống")
    private String credential; // JWT credential from Google One Tap
}
