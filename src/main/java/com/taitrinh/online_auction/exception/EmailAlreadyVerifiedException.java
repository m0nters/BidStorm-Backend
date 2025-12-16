package com.taitrinh.online_auction.exception;

public class EmailAlreadyVerifiedException extends RuntimeException {
    public EmailAlreadyVerifiedException() {
        super("Email đã được xác thực");
    }

    public EmailAlreadyVerifiedException(String email) {
        super("Email đã được xác thực: " + email);
    }
}
