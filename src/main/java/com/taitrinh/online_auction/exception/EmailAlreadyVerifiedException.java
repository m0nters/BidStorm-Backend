package com.taitrinh.online_auction.exception;

public class EmailAlreadyVerifiedException extends RuntimeException {
    public EmailAlreadyVerifiedException() {
        super("Email is already verified");
    }

    public EmailAlreadyVerifiedException(String email) {
        super("Email is already verified: " + email);
    }
}
