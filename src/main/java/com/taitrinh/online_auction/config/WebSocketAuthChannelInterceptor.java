package com.taitrinh.online_auction.config;

import java.util.List;

import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetailsService;

import com.taitrinh.online_auction.repository.ProductRepository;
import com.taitrinh.online_auction.security.JwtUtil;
import com.taitrinh.online_auction.security.UserDetailsImpl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Configuration
@Order(Ordered.HIGHEST_PRECEDENCE + 99)
@RequiredArgsConstructor
@Slf4j
public class WebSocketAuthChannelInterceptor implements ChannelInterceptor {

    private final JwtUtil jwtUtil;
    private final UserDetailsService userDetailsService;
    private final ProductRepository productRepository;

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);

        if (accessor != null) {
            if (StompCommand.CONNECT.equals(accessor.getCommand())) {
                // Extract JWT from connect headers
                List<String> authorization = accessor.getNativeHeader("Authorization");

                if (authorization != null && !authorization.isEmpty()) {
                    String token = authorization.get(0);
                    if (token.startsWith("Bearer ")) {
                        token = token.substring(7);
                    }

                    try {
                        if (jwtUtil.validateToken(token)) {
                            String username = jwtUtil.extractUsername(token);
                            UserDetailsImpl userDetails = (UserDetailsImpl) userDetailsService
                                    .loadUserByUsername(username);

                            Authentication auth = new UsernamePasswordAuthenticationToken(
                                    userDetails,
                                    null,
                                    userDetails.getAuthorities());

                            accessor.setUser(auth);
                            SecurityContextHolder.getContext().setAuthentication(auth);

                            log.debug("WebSocket authenticated user: {}", username);
                        }
                    } catch (Exception e) {
                        log.warn("WebSocket authentication failed: {}", e.getMessage());
                    }
                }
            } else if (StompCommand.SUBSCRIBE.equals(accessor.getCommand())) {
                String destination = accessor.getDestination();

                // Check if subscribing to seller-only channel
                if (destination != null && destination.matches("/topic/product/\\d+/comments/seller")) {
                    Authentication auth = (Authentication) accessor.getUser();

                    if (auth == null || !(auth.getPrincipal() instanceof UserDetailsImpl)) {
                        log.warn("Unauthorized subscription attempt to seller channel: {}", destination);
                        throw new SecurityException("Authentication required for seller channel");
                    }

                    // Extract productId from destination
                    String[] parts = destination.split("/");
                    Long productId = Long.parseLong(parts[3]);

                    UserDetailsImpl userDetails = (UserDetailsImpl) auth.getPrincipal();
                    Long userId = userDetails.getUserId();

                    // Verify user is the seller of this product
                    boolean isSeller = productRepository.findById(productId)
                            .map(product -> product.getSeller() != null &&
                                    product.getSeller().getId().equals(userId))
                            .orElse(false);

                    if (!isSeller) {
                        log.warn(
                                "User {} attempted to subscribe to seller channel for product {} but is not the seller",
                                userId, productId);
                        throw new SecurityException("Only product seller can subscribe to seller channel");
                    }

                    log.debug("Seller {} authorized for seller channel of product {}", userId, productId);
                }
            }
        }

        return message;
    }
}
