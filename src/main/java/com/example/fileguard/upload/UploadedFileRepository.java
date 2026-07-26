package com.example.fileguard.upload;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface UploadedFileRepository extends JpaRepository<UploadedFile, Long> {

    List<UploadedFile> findAllByStatusOrderByCreatedAtDesc(UploadStatus status);
}
