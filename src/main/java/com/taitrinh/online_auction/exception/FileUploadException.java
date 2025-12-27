package com.taitrinh.online_auction.exception;

/**
 * Custom exception for file upload errors
 * Thrown when S3 upload fails, invalid file type, or file size exceeded
 */
public class FileUploadException extends RuntimeException {

    public FileUploadException(String message) {
        super(message);
    }

    public FileUploadException(String message, Throwable cause) {
        super(message, cause);
    }
}
