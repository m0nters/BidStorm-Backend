package com.taitrinh.online_auction.exception;

public class ResourceNotFoundException extends RuntimeException {
    public ResourceNotFoundException(String resource, String identifier) {
        super(resource + " not found: " + identifier);
    }

    public ResourceNotFoundException(String message) {
        super(message);
    }
}
