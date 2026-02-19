package com.woorido.django.brix.service;

import java.time.LocalDateTime;
import java.time.ZoneId;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class BrixBatchScheduler {

  @Value("${brix.batch.zone:Asia/Seoul}")
  private String batchZone;

  private final BrixBatchService brixBatchService;

  @Scheduled(cron = "${brix.batch.cron:0 0 3 1 * *}", zone = "${brix.batch.zone:Asia/Seoul}")
  public void runMonthlyBrixBatch() {
    LocalDateTime cutoffAt = LocalDateTime.now(ZoneId.of(batchZone));
    var result = brixBatchService.recalculate(cutoffAt);
    log.info("[BRIX] monthly batch completed: cutoffAt={}, requestedUsers={}, updatedUsers={}",
        result.getCutoffAt(), result.getRequestedUsers(), result.getUpdatedUsers());
  }
}
