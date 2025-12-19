package com.taitrinh.online_auction.service;

import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import com.taitrinh.online_auction.dto.comment.CommentResponse;
import com.taitrinh.online_auction.dto.websocket.CommentEvent;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class CommentNotificationService {

    private final SimpMessagingTemplate messagingTemplate;

    /**
     * Notify all subscribers about a new comment on a product
     */
    public void notifyNewComment(Long productId, CommentResponse comment) {
        log.debug("Broadcasting new comment for product: {} comment id: {}", productId, comment.getId());

        CommentEvent event = CommentEvent.newComment(productId, comment);
        String destination = "/topic/product/" + productId + "/comments";

        messagingTemplate.convertAndSend(destination, event);

        log.info("Broadcasted new comment to: {}", destination);
    }

    /**
     * Notify all subscribers about a deleted comment
     */
    public void notifyDeleteComment(Long productId, Long commentId) {
        log.debug("Broadcasting comment deletion for product: {} comment id: {}", productId, commentId);

        CommentEvent event = CommentEvent.deleteComment(productId, commentId);
        String destination = "/topic/product/" + productId + "/comments";

        messagingTemplate.convertAndSend(destination, event);

        log.info("Broadcasted comment deletion to: {}", destination);
    }
}
