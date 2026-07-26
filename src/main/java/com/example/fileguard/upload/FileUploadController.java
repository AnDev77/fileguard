package com.example.fileguard.upload;

import com.example.fileguard.common.ApiResponse;
import com.example.fileguard.upload.dto.FileDownloadResource;
import com.example.fileguard.upload.dto.FileUploadResponse;
import org.springframework.core.io.Resource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.nio.charset.StandardCharsets;
import java.util.List;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/files")
public class FileUploadController {

    private final FileUploadService fileUploadService;

    public FileUploadController(FileUploadService fileUploadService) {
        this.fileUploadService = fileUploadService;
    }

    @PostMapping
    public ApiResponse<FileUploadResponse> upload(@RequestParam("file") MultipartFile file) {
        return ApiResponse.ok(fileUploadService.upload(file), "파일 업로드에 성공했습니다.");
    }

    @GetMapping
    public ApiResponse<List<FileUploadResponse>> getStoredFiles() {
        return ApiResponse.ok(fileUploadService.getStoredFiles());
    }

    @GetMapping("/{id}/download")
    public ResponseEntity<Resource> download(@org.springframework.web.bind.annotation.PathVariable Long id) {
        FileDownloadResource download = fileUploadService.getDownload(id);
        ContentDisposition disposition = ContentDisposition.attachment()
                .filename(download.originalFilename(), StandardCharsets.UTF_8)
                .build();

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(download.contentType()))
                .contentLength(download.size())
                .header(HttpHeaders.CONTENT_DISPOSITION, disposition.toString())
                .header("X-Content-Type-Options", "nosniff")
                .body(download.resource());
    }
}
