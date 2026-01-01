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
public class ChatMessageRequest {

    @NotBlank(message = "Tin nhắn không được để trống")
    @Size(max = 2000, message = "Tin nhắn không được quá 2000 ký tự")
    private String message;
}
