package com.taitrinh.online_auction.dto.websocket;

import com.taitrinh.online_auction.dto.comment.CommentResponse;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CommentEvent {

    private EventType type;
    private Long productId;
    private CommentResponse comment; // For NEW_COMMENT
    private Long commentId; // For DELETE_COMMENT

    public enum EventType {
        NEW_COMMENT,
        DELETE_COMMENT
    }

    // Factory methods for convenience
    public static CommentEvent newComment(Long productId, CommentResponse comment) {
        return CommentEvent.builder()
                .type(EventType.NEW_COMMENT)
                .productId(productId)
                .comment(comment)
                .build();
    }

    public static CommentEvent deleteComment(Long productId, Long commentId) {
        return CommentEvent.builder()
                .type(EventType.DELETE_COMMENT)
                .productId(productId)
                .commentId(commentId)
                .build();
    }
}
