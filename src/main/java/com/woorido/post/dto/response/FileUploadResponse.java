package com.woorido.post.dto.response;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class FileUploadResponse {
    private Long fileId;
    private String fileName;
    private String fileUrl;
    private Long fileSize;
    private String contentType;
}
