package com.taitrinh.online_auction.exception;

/**
 * Exception thrown when a user attempts to perform a seller-only action
 * without proper seller permissions or when trying to modify resources
 * they don't own.
 */
public class UnauthorizedSellerException extends RuntimeException {
    public UnauthorizedSellerException(String message) {
        super(message);
    }
}
