package com.woorido.common.strategy;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

@Component
public class LocalImageUploadStrategy implements ImageUploadStrategy {

    @Value("${file.upload.dir:uploads}")
    private String uploadDir;

    @Override
    public String upload(MultipartFile file, String subPath) {
        try {
            // Validate File Extension
            String originalFilename = file.getOriginalFilename();
            if (originalFilename == null || !originalFilename.contains(".")) {
                throw new IllegalArgumentException("POST_007: 지원하지 않는 파일 형식입니다");
            }
            String extension = originalFilename.substring(originalFilename.lastIndexOf(".")).toLowerCase();
            if (!isValidExtension(extension)) {
                throw new IllegalArgumentException("POST_007: 지원하지 않는 파일 형식입니다");
            }

            // Create directory if not exists
            Path rootLocation = Paths.get(uploadDir).toAbsolutePath().normalize();
            Path targetDir = rootLocation;
            if (subPath != null && !subPath.isEmpty()) {
                targetDir = rootLocation.resolve(subPath);
            }
            Files.createDirectories(targetDir);

            // Generate unique filename
            String savedFilename = UUID.randomUUID().toString() + extension;

            // Save file
            Path targetLocation = targetDir.resolve(savedFilename);
            Files.copy(file.getInputStream(), targetLocation);

            // Return relative path or URL (depends on serving strategy)
            // Returning relative path for now
            return (subPath != null ? subPath + "/" : "") + savedFilename;

        } catch (IOException e) {
            throw new RuntimeException("FILE_001: 파일 업로드 중 오류가 발생했습니다", e);
        }
    }

    private boolean isValidExtension(String extension) {
        String[] supportedExtensions = {
                ".jpg", ".jpeg", ".png", ".gif", ".webp",
                ".pdf", ".doc", ".docx", ".xls", ".xlsx", ".ppt", ".pptx"
        };
        for (String ext : supportedExtensions) {
            if (ext.equals(extension)) {
                return true;
            }
        }
        return false;
    }
}
