package com.example.fileguard.common.error;

import org.springframework.http.HttpStatus;

public enum ErrorCode {
    INVALID_EXTENSION(HttpStatus.BAD_REQUEST, "Invalid extension format."),
    EXTENSION_ALREADY_EXISTS(HttpStatus.CONFLICT, "Extension already exists."),
    EXTENSION_NOT_FOUND(HttpStatus.NOT_FOUND, "Extension policy was not found."),
    CUSTOM_EXTENSION_LIMIT_EXCEEDED(HttpStatus.BAD_REQUEST, "Custom extension limit exceeded."),
    FIXED_EXTENSION_NOT_DELETABLE(HttpStatus.BAD_REQUEST, "Fixed extensions cannot be deleted."),
    BLOCKED_FILE_EXTENSION(HttpStatus.BAD_REQUEST, "File extension is blocked."),
    EMPTY_FILE(HttpStatus.BAD_REQUEST, "Empty file cannot be uploaded."),
    FILENAME_TOO_LONG(HttpStatus.BAD_REQUEST, "Filename is too long."),
    FILE_EXTENSION_REQUIRED(HttpStatus.BAD_REQUEST, "File extension is required."),
    FILE_SIZE_EXCEEDED(HttpStatus.PAYLOAD_TOO_LARGE, "File size limit exceeded."),
    FILE_NOT_FOUND(HttpStatus.NOT_FOUND, "Stored file was not found."),
    FILE_STORAGE_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "File storage failed."),
    VALIDATION_FAILED(HttpStatus.BAD_REQUEST, "Request validation failed.");

    private final HttpStatus status;
    private final String message;

    ErrorCode(HttpStatus status, String message) {
        this.status = status;
        this.message = message;
    }

    public HttpStatus status() {
        return status;
    }

    public String message() {
        return message;
    }
}
