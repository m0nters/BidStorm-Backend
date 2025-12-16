package com.taitrinh.online_auction.exception;

public class AccountInactiveException extends RuntimeException {
    public AccountInactiveException() {
        super("Tài khoản đang bị khóa");
    }

    public AccountInactiveException(String message) {
        super(message);
    }
}
