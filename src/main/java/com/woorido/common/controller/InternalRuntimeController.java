package com.woorido.common.controller;

import com.woorido.common.dto.ApiResponse;
import com.woorido.common.dto.RuntimeInfoResponse;
import com.woorido.common.image.ImagePolicyType;
import com.woorido.django.ledger.client.DjangoLedgerClient;
import java.time.Instant;
import java.util.Arrays;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.info.BuildProperties;
import org.springframework.boot.info.GitProperties;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/internal/runtime")
@ConditionalOnProperty(prefix = "internal.runtime", name = "enabled", havingValue = "true")
public class InternalRuntimeController {

  private final Environment environment;
  private final ObjectProvider<BuildProperties> buildPropertiesProvider;
  private final ObjectProvider<GitProperties> gitPropertiesProvider;
  private final Instant startedAt = Instant.now();
  private final String internalApiKey;
  private final String uploadDir;
  private final String djangoBaseUrl;
  private final DjangoLedgerClient djangoLedgerClient;

  public InternalRuntimeController(
      Environment environment,
      ObjectProvider<BuildProperties> buildPropertiesProvider,
      ObjectProvider<GitProperties> gitPropertiesProvider,
      DjangoLedgerClient djangoLedgerClient,
      @Value("${django.brix.api-key:}") String internalApiKey,
      @Value("${file.upload.dir:uploads}") String uploadDir,
      @Value("${django.brix.base-url:http://127.0.0.1:8000}") String djangoBaseUrl) {
    this.environment = environment;
    this.buildPropertiesProvider = buildPropertiesProvider;
    this.gitPropertiesProvider = gitPropertiesProvider;
    this.djangoLedgerClient = djangoLedgerClient;
    this.internalApiKey = internalApiKey;
    this.uploadDir = uploadDir;
    this.djangoBaseUrl = djangoBaseUrl;
  }

  @GetMapping("/info")
  public ResponseEntity<ApiResponse<RuntimeInfoResponse>> getRuntimeInfo(
      @RequestHeader(value = "X-Internal-Api-Key", required = false) String requestApiKey) {
    if (internalApiKey == null || internalApiKey.isBlank()
        || requestApiKey == null
        || !requestApiKey.equals(internalApiKey)) {
      return ResponseEntity.status(HttpStatus.FORBIDDEN)
          .body(ApiResponse.error("AUTH_001:인증이 필요합니다"));
    }

    BuildProperties buildProperties = buildPropertiesProvider.getIfAvailable();
    GitProperties gitProperties = gitPropertiesProvider.getIfAvailable();

    RuntimeInfoResponse response = RuntimeInfoResponse.builder()
        .gitCommit(resolveGitCommit(gitProperties))
        .gitBranch(resolveGitBranch(gitProperties))
        .buildTime(resolveBuildTime(buildProperties))
        .startedAt(startedAt.toString())
        .activeProfiles(Arrays.asList(environment.getActiveProfiles()))
        .uploadDir(uploadDir)
        .djangoBaseUrl(djangoBaseUrl)
        .ledgerDjangoHealth(djangoLedgerClient.getLedgerDjangoHealth())
        .lastCheckedAt(djangoLedgerClient.getLastCheckedAt())
        .lastErrorCode(djangoLedgerClient.getLastErrorCode())
        .uploadPolicy(RuntimeInfoResponse.UploadPolicy.builder()
            .post(toPolicyItem(ImagePolicyType.POST_ATTACHMENT))
            .banner(toPolicyItem(ImagePolicyType.CHALLENGE_BANNER))
            .thumbnail(toPolicyItem(ImagePolicyType.CHALLENGE_THUMBNAIL))
            .profile(toPolicyItem(ImagePolicyType.USER_PROFILE))
            .build())
        .build();

    return ResponseEntity.ok(ApiResponse.success(response));
  }

  private RuntimeInfoResponse.PolicyItem toPolicyItem(ImagePolicyType policyType) {
    ImagePolicyType.ImageDimensionRule dimensionRule = policyType.getDimensionRule();
    String resolution = null;
    if (dimensionRule != null) {
      resolution = dimensionRule.exactMatch()
          ? dimensionRule.width() + "x" + dimensionRule.height()
          : "min " + dimensionRule.width() + "x" + dimensionRule.height();
    }

    return RuntimeInfoResponse.PolicyItem.builder()
        .maxCount(policyType.getMaxFileCount())
        .maxFileSizeBytes(policyType.getMaxFileSizeBytes())
        .resolution(resolution)
        .build();
  }

  private String resolveGitCommit(GitProperties gitProperties) {
    if (gitProperties == null) {
      return "unknown";
    }
    String commit = gitProperties.getShortCommitId();
    if (commit == null || commit.isBlank()) {
      commit = gitProperties.get("commit.id");
    }
    return commit == null || commit.isBlank() ? "unknown" : commit;
  }

  private String resolveGitBranch(GitProperties gitProperties) {
    if (gitProperties == null) {
      return "unknown";
    }
    String branch = gitProperties.getBranch();
    return branch == null || branch.isBlank() ? "unknown" : branch;
  }

  private String resolveBuildTime(BuildProperties buildProperties) {
    if (buildProperties == null || buildProperties.getTime() == null) {
      return "unknown";
    }
    return buildProperties.getTime().toString();
  }
}
