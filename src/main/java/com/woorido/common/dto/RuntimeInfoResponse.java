package com.woorido.common.dto;

import java.util.List;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class RuntimeInfoResponse {
  private String gitCommit;
  private String gitBranch;
  private String buildTime;
  private String startedAt;
  private List<String> activeProfiles;
  private UploadPolicy uploadPolicy;
  private String uploadDir;
  private String djangoBaseUrl;
  private String ledgerDjangoHealth;
  private String lastCheckedAt;
  private String lastErrorCode;

  @Getter
  @Builder
  public static class UploadPolicy {
    private PolicyItem post;
    private PolicyItem banner;
    private PolicyItem thumbnail;
    private PolicyItem profile;
  }

  @Getter
  @Builder
  public static class PolicyItem {
    private int maxCount;
    private long maxFileSizeBytes;
    private String resolution;
  }
}
