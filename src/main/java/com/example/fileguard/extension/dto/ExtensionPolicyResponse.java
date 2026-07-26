package com.example.fileguard.extension.dto;

import com.example.fileguard.extension.ExtensionPolicy;

import java.time.LocalDateTime;

public record ExtensionPolicyResponse(
        Long id,
        String extension,
        boolean fixed,
        boolean blocked,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {

    public static ExtensionPolicyResponse from(ExtensionPolicy policy) {
        return new ExtensionPolicyResponse(
                policy.getId(),
                policy.getExtension(),
                policy.isFixed(),
                policy.isBlocked(),
                policy.getCreatedAt(),
                policy.getUpdatedAt()
        );
    }
}
