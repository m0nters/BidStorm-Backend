package com.taitrinh.online_auction.exception;

public class AccountInactiveException extends RuntimeException {
    public AccountInactiveException() {
        super("Tài khoản đã bị khóa. Vui lòng liên hệ với quản trị viên nếu bạn nghĩ đây là một sự nhầm lẫn");
    }

    public AccountInactiveException(String message) {
        super(message);
    }
}
