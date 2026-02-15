package com.taitrinh.online_auction.exception.comment;

public class UnauthorizedCommentActionException extends RuntimeException {
    public UnauthorizedCommentActionException(String message) {
        super(message);
    }
}
