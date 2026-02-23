package com.woorido.common.image;

public enum ImagePolicyType {
  POST_ATTACHMENT(10, 20L * 1024 * 1024, null),
  CHALLENGE_BANNER(1, 6L * 1024 * 1024, new ImageDimensionRule(true, 2048, 1152)),
  CHALLENGE_THUMBNAIL(1, 4L * 1024 * 1024, new ImageDimensionRule(false, 128, 128)),
  USER_PROFILE(1, 4L * 1024 * 1024, new ImageDimensionRule(false, 128, 128));

  private final int maxFileCount;
  private final long maxFileSizeBytes;
  private final ImageDimensionRule dimensionRule;

  ImagePolicyType(int maxFileCount, long maxFileSizeBytes, ImageDimensionRule dimensionRule) {
    this.maxFileCount = maxFileCount;
    this.maxFileSizeBytes = maxFileSizeBytes;
    this.dimensionRule = dimensionRule;
  }

  public int getMaxFileCount() {
    return maxFileCount;
  }

  public long getMaxFileSizeBytes() {
    return maxFileSizeBytes;
  }

  public ImageDimensionRule getDimensionRule() {
    return dimensionRule;
  }

  public record ImageDimensionRule(boolean exactMatch, int width, int height) {
  }
}
