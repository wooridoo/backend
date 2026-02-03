-- woorido.meeting_votes 테이블 확장 (범용 투표 지원을 위해)
ALTER TABLE woorido.meeting_votes
ADD (
    challenge_id VARCHAR2(36),
    type VARCHAR2(20),
    title VARCHAR2(100),
    description VARCHAR2(500),
    target_id VARCHAR2(36),
    created_by VARCHAR2(36)
  );
-- 기존 레코드에 대한 기본값 설정 (모임 투표로 간주)
UPDATE woorido.meeting_votes
SET type = 'MEETING_ATTENDANCE',
  title = '모임 참석 투표'
WHERE type IS NULL;
-- 인덱스 추가
CREATE INDEX idx_meeting_votes_challenge ON woorido.meeting_votes(challenge_id);