package com.taitrinh.online_auction.dto;

import java.time.ZonedDateTime;

import com.fasterxml.jackson.annotation.JsonInclude;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Standard API response wrapper")
public class ApiResponse<T> {

    private boolean success;
    private int status;
    private String message;
    private T data;
    private ZonedDateTime timestamp;

    // Success response factory methods
    public static <T> ApiResponse<T> success(T data, int status, String message) {
        return ApiResponse.<T>builder()
                .success(true)
                .status(status)
                .message(message)
                .data(data)
                .timestamp(ZonedDateTime.now())
                .build();
    }

    public static <T> ApiResponse<T> success(T data, int status) {
        return ApiResponse.<T>builder()
                .success(true)
                .status(status)
                .data(data)
                .timestamp(ZonedDateTime.now())
                .build();
    }

    public static <T> ApiResponse<T> ok(T data) {
        return success(data, 200);
    }

    public static <T> ApiResponse<T> ok(T data, String message) {
        return success(data, 200, message);
    }

    public static <T> ApiResponse<T> created(T data) {
        return success(data, 201);
    }

    public static <T> ApiResponse<T> created(T data, String message) {
        return success(data, 201, message);
    }
}