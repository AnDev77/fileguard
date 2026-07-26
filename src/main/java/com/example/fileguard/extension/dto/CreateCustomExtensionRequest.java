package com.example.fileguard.extension.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateCustomExtensionRequest(
        @NotBlank(message = "확장자는 필수입니다.")
        @Size(max = 20, message = "확장자는 최대 20자입니다.")
        String extension
) {
}
