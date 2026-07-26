package com.example.fileguard.common.error;

import java.time.Instant;
import java.util.List;

public record ErrorResponse(
        String code,
        String message,
        List<String> details,
        Instant timestamp
) {

    public static ErrorResponse of(ErrorCode errorCode) {
        return new ErrorResponse(errorCode.name(), errorCode.message(), List.of(), Instant.now());
    }

    public static ErrorResponse of(ErrorCode errorCode, String message) {
        return new ErrorResponse(errorCode.name(), message, List.of(), Instant.now());
    }

    public static ErrorResponse of(ErrorCode errorCode, List<String> details) {
        return new ErrorResponse(errorCode.name(), errorCode.message(), details, Instant.now());
    }
}
