package com.example.fileguard.upload.dto;

import org.springframework.core.io.Resource;

public record FileDownloadResource(
        Resource resource,
        String originalFilename,
        String contentType,
        long size
) {
}
