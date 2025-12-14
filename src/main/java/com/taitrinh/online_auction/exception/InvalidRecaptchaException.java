package com.taitrinh.online_auction.exception;

public class InvalidRecaptchaException extends RuntimeException {

    public InvalidRecaptchaException() {
        super("Invalid reCAPTCHA verification");
    }

    public InvalidRecaptchaException(String message) {
        super(message);
    }
}
