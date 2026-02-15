package com.taitrinh.online_auction.exception.auth;

public class InvalidOtpException extends RuntimeException {
    public InvalidOtpException() {
        super("OTP không hợp lệ, đã cũ hoặc đã hết hạn. Vui lòng yêu cầu lại OTP.");
    }

    public InvalidOtpException(String message) {
        super(message);
    }
}
