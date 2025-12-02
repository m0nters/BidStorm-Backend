package com.taitrinh.online_auction.dto;

import java.time.ZonedDateTime;
import java.util.List;

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
@Schema(description = "Error response wrapper")
public class ErrorResponse {

    private boolean success;
    private int status;
    private String error;
    private String message;
    private List<ValidationError> details;
    private ZonedDateTime timestamp;

    public static ErrorResponse of(int status, String error, String message) {
        return ErrorResponse.builder()
                .success(false)
                .status(status)
                .error(error)
                .message(message)
                .timestamp(ZonedDateTime.now())
                .build();
    }

    public static ErrorResponse of(int status, String error, String message, List<ValidationError> details) {
        return ErrorResponse.builder()
                .success(false)
                .status(status)
                .error(error)
                .message(message)
                .details(details)
                .timestamp(ZonedDateTime.now())
                .build();
    }
}