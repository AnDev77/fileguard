package com.example.fileguard.upload.dto;

import com.example.fileguard.upload.UploadStatus;
import com.example.fileguard.upload.UploadedFile;

import java.time.LocalDateTime;

public record FileUploadResponse(
        Long id,
        String originalFilename,
        String storedFilename,
        String extension,
        long size,
        UploadStatus status,
        String rejectReason,
        LocalDateTime createdAt
) {

    public static FileUploadResponse from(UploadedFile file) {
        return new FileUploadResponse(
                file.getId(),
                file.getOriginalFilename(),
                file.getStoredFilename(),
                file.getExtension(),
                file.getSize(),
                file.getStatus(),
                file.getRejectReason(),
                file.getCreatedAt()
        );
    }
}
