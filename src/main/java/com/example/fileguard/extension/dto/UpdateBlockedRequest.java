package com.example.fileguard.extension.dto;

import jakarta.validation.constraints.NotNull;

public record UpdateBlockedRequest(
        @NotNull(message = "blocked 값은 필수입니다.")
        Boolean blocked
) {
}
