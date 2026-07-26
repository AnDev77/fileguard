package com.example.fileguard.extension;

import com.example.fileguard.common.error.BusinessException;
import com.example.fileguard.common.error.ErrorCode;

import java.util.Locale;
import java.util.regex.Pattern;

public final class ExtensionNormalizer {

    private static final Pattern ALLOWED_EXTENSION = Pattern.compile("^[a-z0-9]{1,20}$");

    private ExtensionNormalizer() {
    }

    public static String normalize(String rawExtension) {
        if (rawExtension == null) {
            throw new BusinessException(ErrorCode.INVALID_EXTENSION);
        }

        String normalized = rawExtension.trim().toLowerCase(Locale.ROOT);
        while (normalized.startsWith(".")) {
            normalized = normalized.substring(1);
        }

        if (!ALLOWED_EXTENSION.matcher(normalized).matches()) {
            throw new BusinessException(ErrorCode.INVALID_EXTENSION);
        }

        return normalized;
    }

    public static String extractFromFilename(String filename) {
        if (filename == null || filename.isBlank()) {
            return null;
        }

        String cleanName = filename.replace("\\", "/");
        cleanName = cleanName.substring(cleanName.lastIndexOf('/') + 1);
        int dotIndex = cleanName.lastIndexOf('.');
        if (dotIndex < 0 || dotIndex == cleanName.length() - 1) {
            return null;
        }

        return normalize(cleanName.substring(dotIndex + 1));
    }
}
