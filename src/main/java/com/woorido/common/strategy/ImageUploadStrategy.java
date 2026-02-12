package com.woorido.common.strategy;

import org.springframework.web.multipart.MultipartFile;

public interface ImageUploadStrategy {
    String upload(MultipartFile file, String subPath);
}
