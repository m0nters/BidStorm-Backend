package com.taitrinh.online_auction.exception;

public class InvalidRefreshTokenException extends RuntimeException {
    public InvalidRefreshTokenException() {
        super("Token refresh không hợp lệ hoặc đã hết hạn");
    }

    public InvalidRefreshTokenException(String message) {
        super(message);
    }
}
