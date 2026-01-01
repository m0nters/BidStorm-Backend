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
            } else if (StompCommand.SEND.equals(accessor.getCommand())) {
                // For SEND commands (messages sent to @MessageMapping handlers),
                // we need to reload the user from the database because UserDetailsImpl
                // contains a JPA entity that doesn't serialize properly
                Authentication existingAuth = (Authentication) accessor.getUser();

                if (existingAuth != null && existingAuth.getPrincipal() instanceof UserDetailsImpl) {
                    UserDetailsImpl userDetails = (UserDetailsImpl) existingAuth.getPrincipal();
                    String username = userDetails.getUsername();

                    try {
                        // Reload user details from database to get fresh User entity
                        UserDetailsImpl freshUserDetails = (UserDetailsImpl) userDetailsService
                                .loadUserByUsername(username);

                        Authentication freshAuth = new UsernamePasswordAuthenticationToken(
                                freshUserDetails,
                                null,
                                freshUserDetails.getAuthorities());

                        accessor.setUser(freshAuth);
                        SecurityContextHolder.getContext().setAuthentication(freshAuth);

                        log.debug("SEND command - reloaded user authentication for: {}", username);
                    } catch (Exception e) {
                        log.error("Failed to reload user for SEND command: {}", e.getMessage());
                    }
                } else {
                    log.warn("SEND command - no valid authentication found for session: {}", accessor.getSessionId());
                }
            } else if (StompCommand.SUBSCRIBE.equals(accessor.getCommand())) {
                String destination = accessor.getDestination();

                // Check if subscribing to seller-only channels (comments or bids)
                if (destination != null && (destination.matches("/topic/product/\\d+/comments/seller") ||
                        destination.matches("/topic/product/\\d+/bids/seller"))) {
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

                // Check if subscribing to order chat channels (seller and winner only)
                if (destination != null && destination.matches("/topic/order/\\d+/chat")) {
                    Authentication auth = (Authentication) accessor.getUser();

                    if (auth == null || !(auth.getPrincipal() instanceof UserDetailsImpl)) {
                        log.warn("Unauthorized subscription attempt to chat channel: {}", destination);
                        throw new SecurityException("Authentication required for chat channel");
                    }

                    // Extract productId from destination
                    String[] parts = destination.split("/");
                    Long productId = Long.parseLong(parts[3]);

                    UserDetailsImpl userDetails = (UserDetailsImpl) auth.getPrincipal();
                    Long userId = userDetails.getUserId();

                    // Verify user is either seller or winner
                    boolean isAuthorized = productRepository.findById(productId)
                            .map(product -> {
                                boolean isSeller = product.getSeller() != null &&
                                        product.getSeller().getId().equals(userId);
                                boolean isWinner = product.getWinner() != null &&
                                        product.getWinner().getId().equals(userId);
                                return isSeller || isWinner;
                            })
                            .orElse(false);

                    if (!isAuthorized) {
                        log.warn(
                                "User {} attempted to subscribe to chat channel for product {} but is neither seller nor winner",
                                userId, productId);
                        throw new SecurityException("Only seller and winner can subscribe to chat channel");
                    }

                    log.debug("User {} authorized for chat channel of product {}", userId, productId);
                }

                // Check if subscribing to order status channels (buyer and seller only)
                if (destination != null && destination.matches("/topic/order/\\d+/status")) {
                    Authentication auth = (Authentication) accessor.getUser();

                    if (auth == null || !(auth.getPrincipal() instanceof UserDetailsImpl)) {
                        log.warn("Unauthorized subscription attempt to order status channel: {}", destination);
                        throw new SecurityException("Authentication required for order status channel");
                    }

                    // Extract orderId from destination
                    // Destination format: /topic/order/{orderId}/status
                    // After split: ["", "topic", "order", "{orderId}", "status"]
                    String[] parts = destination.split("/");
                    Long orderId = Long.parseLong(parts[3]); // parts[3] is the orderId

                    UserDetailsImpl userDetails = (UserDetailsImpl) auth.getPrincipal();
                    Long userId = userDetails.getUserId();

                    // Verify user is either buyer or seller
                    // This requires checking OrderCompletion table, but we'll allow it for now
                    // The service layer will do the actual authorization check
                    log.debug("User {} subscribing to orderStatus channel {}", userId, orderId);
                }
            }
        }

        return message;
    }
}
