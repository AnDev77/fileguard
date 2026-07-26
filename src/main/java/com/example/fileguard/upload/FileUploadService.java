package com.example.fileguard.upload;

import com.example.fileguard.common.error.BusinessException;
import com.example.fileguard.common.error.ErrorCode;
import com.example.fileguard.extension.ExtensionNormalizer;
import com.example.fileguard.extension.ExtensionPolicyService;
import com.example.fileguard.upload.dto.FileDownloadResource;
import com.example.fileguard.upload.dto.FileUploadResponse;
import org.springframework.core.io.FileSystemResource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.UUID;

@Service
public class FileUploadService {

    private static final int MAX_ORIGINAL_FILENAME_LENGTH = 255;

    private final ExtensionPolicyService extensionPolicyService;
    private final UploadedFileRepository uploadedFileRepository;
    private final Path storageDir;

    public FileUploadService(
            ExtensionPolicyService extensionPolicyService,
            UploadedFileRepository uploadedFileRepository,
            FileStorageProperties properties
    ) {
        this.extensionPolicyService = extensionPolicyService;
        this.uploadedFileRepository = uploadedFileRepository;
        this.storageDir = Path.of(properties.storageDir()).toAbsolutePath().normalize();
    }

    @Transactional(noRollbackFor = BusinessException.class)
    public FileUploadResponse upload(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException(ErrorCode.EMPTY_FILE);
        }

        String originalFilename = sanitizeOriginalFilename(file.getOriginalFilename());
        validateOriginalFilename(originalFilename, file);
        String extension = ExtensionNormalizer.extractFromFilename(originalFilename);

        if (extension == null) {
            rejectUpload(
                    ErrorCode.FILE_EXTENSION_REQUIRED,
                    originalFilename,
                    null,
                    file,
                    "File extension is required."
            );
        }

        if (extensionPolicyService.isBlockedExtension(extension)) {
            String reason = extension + " extension is blocked by upload policy.";
            rejectUpload(ErrorCode.BLOCKED_FILE_EXTENSION, originalFilename, extension, file, reason);
        }

        String storedFilename = createStoredFilename(extension);
        Path target = storageDir.resolve(storedFilename).normalize();
        if (!target.startsWith(storageDir)) {
            throw new BusinessException(ErrorCode.FILE_STORAGE_FAILED);
        }

        try {
            Files.createDirectories(storageDir);
            file.transferTo(target);
        } catch (IOException exception) {
            throw new BusinessException(ErrorCode.FILE_STORAGE_FAILED);
        }

        UploadedFile stored = uploadedFileRepository.save(UploadedFile.stored(
                originalFilename,
                storedFilename,
                extension,
                file.getContentType(),
                file.getSize(),
                target.toString()
        ));
        return FileUploadResponse.from(stored);
    }

    @Transactional(readOnly = true)
    public List<FileUploadResponse> getStoredFiles() {
        return uploadedFileRepository.findAllByStatusOrderByCreatedAtDesc(UploadStatus.STORED)
                .stream()
                .map(FileUploadResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public FileDownloadResource getDownload(Long id) {
        UploadedFile uploadedFile = uploadedFileRepository.findById(id)
                .filter(file -> file.getStatus() == UploadStatus.STORED)
                .orElseThrow(() -> new BusinessException(ErrorCode.FILE_NOT_FOUND));

        Path target = storageDir.resolve(uploadedFile.getStoredFilename()).normalize();
        if (!target.startsWith(storageDir) || !Files.isRegularFile(target)) {
            throw new BusinessException(ErrorCode.FILE_NOT_FOUND);
        }

        String contentType = uploadedFile.getContentType();
        if (contentType == null || contentType.isBlank()) {
            contentType = "application/octet-stream";
        }

        try {
            return new FileDownloadResource(
                    new FileSystemResource(target),
                    uploadedFile.getOriginalFilename(),
                    contentType,
                    Files.size(target)
            );
        } catch (IOException exception) {
            throw new BusinessException(ErrorCode.FILE_NOT_FOUND);
        }
    }

    private void validateOriginalFilename(String originalFilename, MultipartFile file) {
        if (originalFilename.length() > MAX_ORIGINAL_FILENAME_LENGTH) {
            rejectUpload(
                    ErrorCode.FILENAME_TOO_LONG,
                    originalFilename,
                    null,
                    file,
                    "Filename must be " + MAX_ORIGINAL_FILENAME_LENGTH + " characters or less."
            );
        }
    }

    private void rejectUpload(
            ErrorCode errorCode,
            String originalFilename,
            String extension,
            MultipartFile file,
            String reason
    ) {
        UploadedFile rejected = uploadedFileRepository.save(UploadedFile.rejected(
                truncate(originalFilename, MAX_ORIGINAL_FILENAME_LENGTH),
                extension,
                file.getContentType(),
                file.getSize(),
                reason
        ));
        throw new BusinessException(errorCode, reason + " recordId: " + rejected.getId());
    }

    private String sanitizeOriginalFilename(String originalFilename) {
        if (originalFilename == null || originalFilename.isBlank()) {
            return "unknown";
        }

        String cleanName = originalFilename.replace("\\", "/");
        return cleanName.substring(cleanName.lastIndexOf('/') + 1);
    }

    private String truncate(String value, int maxLength) {
        if (value == null || value.length() <= maxLength) {
            return value;
        }
        return value.substring(0, maxLength);
    }

    private String createStoredFilename(String extension) {
        String uuid = UUID.randomUUID().toString();
        if (extension == null || extension.isBlank()) {
            return uuid;
        }
        return uuid + "." + extension;
    }
}
