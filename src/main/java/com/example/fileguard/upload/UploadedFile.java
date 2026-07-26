package com.example.fileguard.upload;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

@Entity
@Table(name = "uploaded_files")
public class UploadedFile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 255)
    private String originalFilename;

    @Column(nullable = false, length = 255)
    private String storedFilename;

    @Column(length = 20)
    private String extension;

    @Column(length = 100)
    private String contentType;

    @Column(nullable = false)
    private long size;

    @Column(nullable = false, length = 500)
    private String storagePath;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private UploadStatus status;

    @Column(length = 255)
    private String rejectReason;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    protected UploadedFile() {
    }

    private UploadedFile(
            String originalFilename,
            String storedFilename,
            String extension,
            String contentType,
            long size,
            String storagePath,
            UploadStatus status,
            String rejectReason
    ) {
        this.originalFilename = originalFilename;
        this.storedFilename = storedFilename;
        this.extension = extension;
        this.contentType = contentType;
        this.size = size;
        this.storagePath = storagePath;
        this.status = status;
        this.rejectReason = rejectReason;
    }

    public static UploadedFile stored(String originalFilename, String storedFilename, String extension, String contentType, long size, String storagePath) {
        return new UploadedFile(originalFilename, storedFilename, extension, contentType, size, storagePath, UploadStatus.STORED, null);
    }

    public static UploadedFile rejected(String originalFilename, String extension, String contentType, long size, String rejectReason) {
        return new UploadedFile(originalFilename, "", extension, contentType, size, "", UploadStatus.REJECTED, rejectReason);
    }

    @PrePersist
    void prePersist() {
        createdAt = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public String getOriginalFilename() {
        return originalFilename;
    }

    public String getStoredFilename() {
        return storedFilename;
    }

    public String getExtension() {
        return extension;
    }

    public String getContentType() {
        return contentType;
    }

    public long getSize() {
        return size;
    }

    public String getStoragePath() {
        return storagePath;
    }

    public UploadStatus getStatus() {
        return status;
    }

    public String getRejectReason() {
        return rejectReason;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}
