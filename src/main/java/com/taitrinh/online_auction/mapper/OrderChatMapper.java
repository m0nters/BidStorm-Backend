package com.taitrinh.online_auction.mapper;

import org.springframework.stereotype.Component;

import com.taitrinh.online_auction.dto.order.ChatMessageResponse;
import com.taitrinh.online_auction.dto.websocket.ChatEvent;
import com.taitrinh.online_auction.entity.OrderChatMessage;

@Component
public class OrderChatMapper {

    public ChatMessageResponse toChatMessageResponse(OrderChatMessage message, Long currentUserId) {
        if (message == null) {
            return null;
        }

        return ChatMessageResponse.builder()
                .id(message.getId())
                .senderId(message.getSender().getId())
                .senderName(message.getSender().getFullName())
                .message(message.getMessage())
                .createdAt(message.getCreatedAt())
                .isOwnMessage(message.getSender().getId().equals(currentUserId))
                .build();
    }

    public ChatEvent toChatEvent(OrderChatMessage message) {
        if (message == null) {
            return null;
        }

        return ChatEvent.builder()
                .id(message.getId())
                .senderId(message.getSender().getId())
                .senderName(message.getSender().getFullName())
                .message(message.getMessage())
                .createdAt(message.getCreatedAt())
                .build();
    }
}
