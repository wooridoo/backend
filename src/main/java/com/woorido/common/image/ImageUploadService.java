package com.woorido.common.image;

import com.woorido.common.strategy.ImageUploadStrategy;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
@RequiredArgsConstructor
public class ImageUploadService {

  private final ImageUploadStrategy imageUploadStrategy;
  private final ImagePolicyValidator imagePolicyValidator;

  @Value("${app.backend.base-url:http://localhost:8080}")
  private String backendBaseUrl;

  public String uploadSingle(MultipartFile file, ImagePolicyType policyType, String subPath) {
    imagePolicyValidator.validate(policyType, file);
    String uploadedPath = imageUploadStrategy.upload(file, subPath);
    return toAbsoluteUploadUrl(uploadedPath);
  }

  public List<String> uploadBatch(List<MultipartFile> files, ImagePolicyType policyType, String subPath) {
    imagePolicyValidator.validate(policyType, files);
    List<String> uploadedUrls = new ArrayList<>();
    for (MultipartFile file : files) {
      String uploadedPath = imageUploadStrategy.upload(file, subPath);
      uploadedUrls.add(toAbsoluteUploadUrl(uploadedPath));
    }
    return uploadedUrls;
  }

  public String toAbsoluteUploadUrl(String uploadedPath) {
    String baseUrl = backendBaseUrl.endsWith("/")
        ? backendBaseUrl.substring(0, backendBaseUrl.length() - 1)
        : backendBaseUrl;
    String normalizedPath = uploadedPath.startsWith("/") ? uploadedPath.substring(1) : uploadedPath;
    return baseUrl + "/uploads/" + normalizedPath;
  }
}
