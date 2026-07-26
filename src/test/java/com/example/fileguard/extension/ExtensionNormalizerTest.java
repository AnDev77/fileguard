package com.example.fileguard.extension;

import com.example.fileguard.common.error.BusinessException;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ExtensionNormalizerTest {

    @Test
    void normalize_removesDotAndLowercases() {
        assertThat(ExtensionNormalizer.normalize(" .EXE ")).isEqualTo("exe");
    }

    @Test
    void normalize_rejectsInvalidCharacters() {
        assertThatThrownBy(() -> ExtensionNormalizer.normalize("ex e"))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    void extractFromFilename_usesLastExtension() {
        assertThat(ExtensionNormalizer.extractFromFilename("file.exe.txt")).isEqualTo("txt");
    }

    @Test
    void extractFromFilename_returnsNullWhenNoExtension() {
        assertThat(ExtensionNormalizer.extractFromFilename("README")).isNull();
    }

    @Test
    void extractFromFilename_treatsLeadingDotNameAsExtension() {
        assertThat(ExtensionNormalizer.extractFromFilename(".env")).isEqualTo("env");
    }

    @Test
    void extractFromFilename_allowsHiddenFilenameWhenRealExtensionExists() {
        assertThat(ExtensionNormalizer.extractFromFilename(".profile.txt")).isEqualTo("txt");
    }
}
