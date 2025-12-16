package com.taitrinh.online_auction.exception;

public class EmailAlreadyExistsException extends RuntimeException {
    public EmailAlreadyExistsException(String email) {
        super("Email đã tồn tại: " + email);
    }
}
