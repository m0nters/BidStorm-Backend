package com.taitrinh.online_auction.exception;

public class AccountInactiveException extends RuntimeException {
    public AccountInactiveException() {
        super("Account is inactive");
    }

    public AccountInactiveException(String message) {
        super(message);
    }
}
