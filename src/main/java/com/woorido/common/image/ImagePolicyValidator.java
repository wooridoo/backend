package com.woorido.common.image;

import com.woorido.common.exception.ImageValidationException;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import javax.imageio.ImageIO;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

@Component
public class ImagePolicyValidator {

  private static final Set<String> ALLOWED_EXTENSIONS = Set.of("jpg", "jpeg", "png", "webp");

  public void validate(ImagePolicyType policyType, MultipartFile file) {
    validate(policyType, List.of(file));
  }

  public void validate(ImagePolicyType policyType, List<MultipartFile> files) {
    if (files == null || files.isEmpty()) {
      throw new ImageValidationException("IMAGE_004", "업로드할 이미지가 없습니다");
    }

    if (files.size() > policyType.getMaxFileCount()) {
      throw new ImageValidationException(
          "IMAGE_004",
          "업로드 가능한 최대 이미지 개수를 초과했습니다");
    }

    for (MultipartFile file : files) {
      validateSingle(policyType, file);
    }
  }

  private void validateSingle(ImagePolicyType policyType, MultipartFile file) {
    if (file == null || file.isEmpty()) {
      throw new ImageValidationException("IMAGE_001", "비어 있는 파일은 업로드할 수 없습니다");
    }

    String originalFilename = file.getOriginalFilename();
    String extension = extractExtension(originalFilename);
    if (extension == null || !ALLOWED_EXTENSIONS.contains(extension)) {
      throw new ImageValidationException("IMAGE_001", "지원하지 않는 이미지 형식입니다");
    }

    String contentType = file.getContentType();
    if (contentType == null || !contentType.toLowerCase(Locale.ROOT).startsWith("image/")) {
      throw new ImageValidationException("IMAGE_001", "이미지 파일만 업로드할 수 있습니다");
    }

    if (file.getSize() > policyType.getMaxFileSizeBytes()) {
      throw new ImageValidationException("IMAGE_002", "허용된 이미지 용량을 초과했습니다");
    }

    ImagePolicyType.ImageDimensionRule dimensionRule = policyType.getDimensionRule();
    if (dimensionRule == null) {
      return;
    }

    BufferedImage bufferedImage = readImage(file);
    int width = bufferedImage.getWidth();
    int height = bufferedImage.getHeight();
    if (dimensionRule.exactMatch()) {
      if (width != dimensionRule.width() || height != dimensionRule.height()) {
        throw new ImageValidationException(
            "IMAGE_003",
            "요구되는 이미지 해상도와 일치하지 않습니다");
      }
      return;
    }

    if (width < dimensionRule.width() || height < dimensionRule.height()) {
      throw new ImageValidationException("IMAGE_003", "요구되는 최소 이미지 해상도보다 작습니다");
    }
  }

  private BufferedImage readImage(MultipartFile file) {
    try {
      BufferedImage bufferedImage = ImageIO.read(file.getInputStream());
      if (bufferedImage == null) {
        throw new ImageValidationException("IMAGE_003", "이미지 해상도를 확인할 수 없습니다");
      }
      return bufferedImage;
    } catch (IOException e) {
      throw new ImageValidationException("IMAGE_003", "이미지 해상도를 확인할 수 없습니다");
    }
  }

  private String extractExtension(String filename) {
    if (filename == null || !filename.contains(".")) {
      return null;
    }
    String extension = filename.substring(filename.lastIndexOf('.') + 1);
    return extension.toLowerCase(Locale.ROOT);
  }
}
