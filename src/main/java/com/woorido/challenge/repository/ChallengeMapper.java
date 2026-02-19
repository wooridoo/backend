package com.woorido.challenge.repository;

import java.util.List;
import java.util.Map;
import java.time.LocalDateTime;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.woorido.challenge.domain.Challenge;

@Mapper
public interface ChallengeMapper {

        // 챌린지 목록 조회 (필터링, 정렬, 페이지네이션)
        List<Map<String, Object>> findAllWithFilter(
                        @Param("status") String status,
                        @Param("category") String category,
                        @Param("sortField") String sortField,
                        @Param("sortDirection") String sortDirection,
                        @Param("offset") int offset,
                        @Param("size") int size);

        // 챌린지 총 개수 조회 (필터링)
        long countAllWithFilter(
                        @Param("status") String status,
                        @Param("category") String category);

        Challenge findById(String id);

        // 챌린지 상세 조회 (리더 정보 포함)
        Map<String, Object> findDetailById(@Param("id") String id);

        // 챌린지 멤버 여부 확인 (존재하면 1, 아니면 0)
        int countMemberByChallengeIdAndUserId(@Param("challengeId") String challengeId, @Param("userId") String userId);

        // 챌린지 잔액 업데이트
        int updateBalance(Challenge challenge); // 반환값으로 낙관적 락 체크

        // 챌린지 생성
        void insert(Challenge challenge);

        // 챌린지명 중복 확인
        int countByName(@Param("name") String name);

        // 챌린지명 중복 확인 (자기 자신 제외)
        int countByNameExcludingId(@Param("name") String name, @Param("challengeId") String challengeId);

        // 리더로 참여중인 챌린지 수 조회
        int countLeaderChallenges(@Param("userId") String userId);

        // 챌린지 리더 여부 확인
        int isLeader(@Param("challengeId") String challengeId, @Param("userId") String userId);

        // 챌린지 정보 수정
        int update(Challenge challenge);

        // 내 챌린지 목록 조회
        List<Map<String, Object>> findMyChallenges(
                        @Param("userId") String userId,
                        @Param("role") String role,
                        @Param("status") String status);

        // 챌린지 어카운트 정보 조회
        Map<String, Object> findChallengeAccount(@Param("challengeId") String challengeId);

        // 최근 장부 내역 조회
        List<Map<String, Object>> findRecentLedgerEntries(
                        @Param("challengeId") String challengeId,
                        @Param("limit") int limit);

        // 챌린지 장부 그래프용 월 범위 원장 조회
        List<Map<String, Object>> findLedgerEntriesForGraph(
                        @Param("challengeId") String challengeId,
                        @Param("startAt") LocalDateTime startAt);

        // 챌린지 상태 및 삭제일시 업데이트 (Soft Delete)
        int updateStatusAndDeletedAt(Challenge challenge);

        // 챌린지 멤버 수 증가
        int incrementCurrentMembers(@Param("challengeId") String challengeId);

        // 챌린지 멤버 수 감소
        int decrementCurrentMembers(@Param("challengeId") String challengeId);

        // 리더 활동 시각 갱신
        int touchLeaderLastActiveAt(@Param("challengeId") String challengeId, @Param("userId") String userId);
}
