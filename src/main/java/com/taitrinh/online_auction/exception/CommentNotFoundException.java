package com.taitrinh.online_auction.exception;

public class CommentNotFoundException extends RuntimeException {
    public CommentNotFoundException(Long commentId) {
        super("Không tìm thấy comment với id: " + commentId);
    }

    public CommentNotFoundException(String message) {
        super(message);
    }
}
