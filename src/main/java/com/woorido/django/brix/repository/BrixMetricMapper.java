package com.woorido.django.brix.repository;

import java.time.LocalDateTime;
import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.woorido.django.brix.model.BrixMetricRow;

@Mapper
public interface BrixMetricMapper {
  List<BrixMetricRow> findMetricsUpTo(@Param("cutoffAt") LocalDateTime cutoffAt);
}
