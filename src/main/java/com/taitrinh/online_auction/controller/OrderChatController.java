package com.taitrinh.online_auction.controller;

import java.security.Principal;
import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.taitrinh.online_auction.dto.ApiResponse;
import com.taitrinh.online_auction.dto.order.ChatMessageRequest;
import com.taitrinh.online_auction.dto.order.ChatMessageResponse;
import com.taitrinh.online_auction.security.UserDetailsImpl;
import com.taitrinh.online_auction.service.OrderChatService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RestController
@Controller
@RequestMapping("/api/v1/orders")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Order Chat", description = "APIs for chat between seller and winner after auction ends")
public class OrderChatController {

    private final OrderChatService orderChatService;

    /**
     * Get chat history for a product
     * Only seller and winner can access
     */
    @GetMapping("/{productId}/chat")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Get chat history", description = "Get all chat messages between seller and winner for a product")
    public ResponseEntity<ApiResponse<List<ChatMessageResponse>>> getChatHistory(
            @PathVariable Long productId,
            @AuthenticationPrincipal UserDetailsImpl userDetails) {

        List<ChatMessageResponse> messages = orderChatService.getChatHistory(productId, userDetails.getUserId());

        return ResponseEntity.ok(ApiResponse.ok(
                messages,
                "Lấy lịch sử tin nhắn thành công"));
    }

    /**
     * Send a chat message via WebSocket
     * Client sends to: /app/order/{productId}/chat
     * Server broadcasts to: /topic/order/{productId}/chat
     */
    @MessageMapping("/order/{productId}/chat")
    public void sendMessage(
            @DestinationVariable Long productId,
            @Valid ChatMessageRequest request,
            Principal principal) {

        // Extract UserDetailsImpl from Principal (set by
        // WebSocketAuthChannelInterceptor)
        UserDetailsImpl userDetails = (UserDetailsImpl) ((UsernamePasswordAuthenticationToken) principal)
                .getPrincipal();

        log.debug("Received chat message for product {} from user {}", productId, userDetails.getUserId());

        // Service will save and broadcast the message
        orderChatService.sendMessage(productId, request.getMessage(), userDetails.getUserId());
    }
}
