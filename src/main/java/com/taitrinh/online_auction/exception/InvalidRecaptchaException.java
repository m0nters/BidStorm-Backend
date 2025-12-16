package com.taitrinh.online_auction.exception;

public class InvalidRecaptchaException extends RuntimeException {

    public InvalidRecaptchaException() {
        super("Xác nhận reCAPTCHA không hợp lệ");
    }

    public InvalidRecaptchaException(String message) {
        super(message);
    }
}
