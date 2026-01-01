package com.taitrinh.online_auction.service;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.taitrinh.online_auction.dto.order.ChatMessageResponse;
import com.taitrinh.online_auction.dto.websocket.ChatEvent;
import com.taitrinh.online_auction.entity.OrderChatMessage;
import com.taitrinh.online_auction.entity.Product;
import com.taitrinh.online_auction.entity.User;
import com.taitrinh.online_auction.exception.ChatAccessDeniedException;
import com.taitrinh.online_auction.exception.ResourceNotFoundException;
import com.taitrinh.online_auction.mapper.OrderChatMapper;
import com.taitrinh.online_auction.repository.OrderChatMessageRepository;
import com.taitrinh.online_auction.repository.ProductRepository;
import com.taitrinh.online_auction.repository.UserRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class OrderChatService {

        private final OrderChatMessageRepository orderChatMessageRepository;
        private final ProductRepository productRepository;
        private final UserRepository userRepository;
        private final OrderChatMapper orderChatMapper;
        private final SimpMessagingTemplate messagingTemplate;

        /**
         * Get chat history for a product
         * Only seller and winner can access the chat
         */
        @Transactional(readOnly = true)
        public List<ChatMessageResponse> getChatHistory(Long productId, Long userId) {
                // Verify product exists
                Product product = productRepository.findById(productId)
                                .orElseThrow(() -> new ResourceNotFoundException("Product", productId));

                // Verify access: user must be seller or winner
                verifyAccess(product, userId);

                // Get all messages
                List<OrderChatMessage> messages = orderChatMessageRepository
                                .findByProduct_IdOrderByCreatedAtAsc(productId);

                // Convert to DTOs
                return messages.stream()
                                .map(message -> orderChatMapper.toChatMessageResponse(message, userId))
                                .collect(Collectors.toList());
        }

        /**
         * Send a chat message
         * Only seller and winner can send messages
         */
        @Transactional
        public ChatMessageResponse sendMessage(Long productId, String message, Long senderId) {
                // Verify product exists
                Product product = productRepository.findById(productId)
                                .orElseThrow(() -> new ResourceNotFoundException("Product", productId));

                // Verify sender exists
                User sender = userRepository.findById(senderId)
                                .orElseThrow(() -> new ResourceNotFoundException("User", senderId));

                // Verify access: sender must be seller or winner
                verifyAccess(product, senderId);

                // Create and save message
                OrderChatMessage chatMessage = OrderChatMessage.builder()
                                .product(product)
                                .sender(sender)
                                .message(message)
                                .build();

                chatMessage = orderChatMessageRepository.save(chatMessage);

                log.info("Chat message sent for product {} by user {}", productId, senderId);

                // Broadcast via WebSocket
                ChatEvent event = orderChatMapper.toChatEvent(chatMessage);
                messagingTemplate.convertAndSend("/topic/order/" + productId + "/chat", event);

                // Return response
                return orderChatMapper.toChatMessageResponse(chatMessage, senderId);
        }

        /**
         * Verify that the user has access to the chat (must be seller or winner)
         */
        private void verifyAccess(Product product, Long userId) {
                boolean isSeller = product.getSeller() != null && product.getSeller().getId().equals(userId);
                boolean isWinner = product.getWinner() != null && product.getWinner().getId().equals(userId);

                if (!isSeller && !isWinner) {
                        log.warn("User {} attempted to access chat for product {} but is neither seller nor winner",
                                        userId, product.getId());
                        throw new ChatAccessDeniedException(product.getId(), userId);
                }
        }
}
