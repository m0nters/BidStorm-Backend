package com.taitrinh.online_auction.dto.websocket;

import java.time.ZonedDateTime;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChatEvent {

    private Long id;
    private Long senderId;
    private String senderName;
    private String message;
    private ZonedDateTime createdAt;
}
