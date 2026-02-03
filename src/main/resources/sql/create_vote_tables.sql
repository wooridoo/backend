-- 투표 테이블 생성
CREATE TABLE woorido.votes (
  id VARCHAR2(36) NOT NULL,
  challenge_id VARCHAR2(36) NOT NULL,
  type VARCHAR2(20) NOT NULL,
  title VARCHAR2(100) NOT NULL,
  description VARCHAR2(500),
  target_id VARCHAR2(36),
  meeting_id VARCHAR2(36),
  status VARCHAR2(20) NOT NULL,
  created_by VARCHAR2(36) NOT NULL,
  deadline TIMESTAMP,
  created_at TIMESTAMP DEFAULT SYSTIMESTAMP,
  completed_at TIMESTAMP,
  CONSTRAINT pk_votes PRIMARY KEY (id)
);
-- 투표 기록(참여) 테이블 생성
CREATE TABLE woorido.vote_records (
  id VARCHAR2(36) NOT NULL,
  vote_id VARCHAR2(36) NOT NULL,
  user_id VARCHAR2(36) NOT NULL,
  choice VARCHAR2(20),
  voted_at TIMESTAMP DEFAULT SYSTIMESTAMP,
  CONSTRAINT pk_vote_records PRIMARY KEY (id)
);
-- 인덱스 생성 (성능 최적화)
CREATE INDEX idx_votes_challenge_id ON woorido.votes(challenge_id);
CREATE INDEX idx_vote_records_vote_id ON woorido.vote_records(vote_id);