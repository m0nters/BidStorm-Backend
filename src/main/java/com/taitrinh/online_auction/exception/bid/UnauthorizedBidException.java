package com.taitrinh.online_auction.exception.bid;

public class UnauthorizedBidException extends RuntimeException {
    public UnauthorizedBidException(String message) {
        super(message);
    }
}
