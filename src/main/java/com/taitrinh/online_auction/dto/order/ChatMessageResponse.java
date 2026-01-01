package com.taitrinh.online_auction.dto.order;

import java.time.ZonedDateTime;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChatMessageResponse {

    private Long id;
    private Long senderId;
    private String senderName;
    private String message;
    private ZonedDateTime createdAt;
    private Boolean isOwnMessage;
}
