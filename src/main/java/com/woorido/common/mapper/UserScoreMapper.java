package com.woorido.common.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import java.math.BigDecimal;

@Mapper
public interface UserScoreMapper {
    /**
     * 사용자 당도 점수 조회
     * 
     * @param userId 사용자 ID
     * @return 당도 점수 (없으면 null)
     */
    BigDecimal findBrixByUserId(@Param("userId") String userId);
}
