package com.taitrinh.online_auction.exception;

public class UnauthorizedBidException extends RuntimeException {
    public UnauthorizedBidException(String message) {
        super(message);
    }
}
