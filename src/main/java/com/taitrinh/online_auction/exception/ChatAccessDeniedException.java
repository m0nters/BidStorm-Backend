package com.taitrinh.online_auction.exception;

public class ChatAccessDeniedException extends RuntimeException {

    public ChatAccessDeniedException(Long productId, Long userId) {
        super(String.format("Người dùng %d không có quyền truy cập chat của sản phẩm %d", userId, productId));
    }

    public ChatAccessDeniedException(String message) {
        super(message);
    }
}
