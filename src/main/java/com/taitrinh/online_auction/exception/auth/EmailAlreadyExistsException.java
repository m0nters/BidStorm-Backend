package com.taitrinh.online_auction.exception.auth;

public class EmailAlreadyExistsException extends RuntimeException {
    public EmailAlreadyExistsException(String email) {
        super("Email đã tồn tại: " + email);
    }
}
