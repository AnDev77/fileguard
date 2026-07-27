package com.example.fileguard.upload;

import com.example.fileguard.common.error.BusinessException;
import com.example.fileguard.common.error.ErrorCode;
import com.example.fileguard.extension.ExtensionPolicyService;
import com.example.fileguard.upload.dto.FileDownloadResource;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FileUploadServiceTest {

    @TempDir
    Path storageDir;

    @Mock
    ExtensionPolicyService extensionPolicyService;

    @Mock
    UploadedFileRepository uploadedFileRepository;

    @Test
    void returnsStoredFileForDownload() throws Exception {
        Path storedPath = storageDir.resolve("stored-file.txt");
        Files.writeString(storedPath, "download content");
        UploadedFile stored = UploadedFile.stored(
                "report.txt",
                "stored-file.txt",
                "txt",
                "text/plain",
                Files.size(storedPath),
                storedPath.toString()
        );
        when(uploadedFileRepository.findById(1L)).thenReturn(Optional.of(stored));

        FileUploadService service = createService();
        FileDownloadResource download = service.getDownload(1L);

        assertThat(download.originalFilename()).isEqualTo("report.txt");
        assertThat(download.contentType()).isEqualTo("text/plain");
        assertThat(download.resource().exists()).isTrue();
        assertThat(download.resource().getInputStream().readAllBytes())
                .isEqualTo("download content".getBytes());
    }

    @Test
    void rejectsDownloadForRejectedUpload() {
        UploadedFile rejected = UploadedFile.rejected(
                "blocked.exe",
                "exe",
                "application/octet-stream",
                10,
                "Blocked extension"
        );
        when(uploadedFileRepository.findById(2L)).thenReturn(Optional.of(rejected));

        FileUploadService service = createService();

        assertThatThrownBy(() -> service.getDownload(2L))
                .isInstanceOfSatisfying(BusinessException.class, exception ->
                        assertThat(exception.errorCode()).isEqualTo(ErrorCode.FILE_NOT_FOUND));
    }

    @Test
    void rejectsDownloadWhenPhysicalFileIsMissing() {
        UploadedFile stored = UploadedFile.stored(
                "missing.txt",
                "missing-file.txt",
                "txt",
                "text/plain",
                10,
                storageDir.resolve("missing-file.txt").toString()
        );
        when(uploadedFileRepository.findById(3L)).thenReturn(Optional.of(stored));

        FileUploadService service = createService();

        assertThatThrownBy(() -> service.getDownload(3L))
                .isInstanceOfSatisfying(BusinessException.class, exception ->
                        assertThat(exception.errorCode()).isEqualTo(ErrorCode.FILE_NOT_FOUND));
    }

    @Test
    void rejectsFileWithoutExtensionBeforeStorageAndRecordsFailure() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "README",
                "text/plain",
                "content".getBytes()
        );
        when(uploadedFileRepository.save(any(UploadedFile.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        FileUploadService service = createService();

        assertThatThrownBy(() -> service.upload(file))
                .isInstanceOfSatisfying(BusinessException.class, exception -> {
                    assertThat(exception.errorCode()).isEqualTo(ErrorCode.FILE_EXTENSION_REQUIRED);
                    assertThat(exception.getMessage()).contains("cannot be evaluated");
                });
        verify(uploadedFileRepository).save(argThat(record ->
                record.getStatus() == UploadStatus.REJECTED
                        && record.getExtension() == null
                        && record.getRejectReason().contains("cannot be evaluated")
        ));
        verifyNoInteractions(extensionPolicyService);
        try (var files = Files.list(storageDir)) {
            assertThat(files).isEmpty();
        }
    }

    private FileUploadService createService() {
        return new FileUploadService(
                extensionPolicyService,
                uploadedFileRepository,
                new FileStorageProperties(storageDir.toString())
        );
    }
}
