-- OAuth 소셜 로그인 스키마 점검용 최소 패치 스크립트
-- 운영 DB 제약이 다를 수 있어, 적용 전 DBA 검토를 권장합니다.

-- 1) password_hash NULL 허용
BEGIN
  EXECUTE IMMEDIATE 'ALTER TABLE woorido.users MODIFY (password_hash NULL)';
EXCEPTION
  WHEN OTHERS THEN
    IF SQLCODE != -1442 THEN -- 이미 동일 속성
      RAISE;
    END IF;
END;
/

-- 2) social_provider 컬럼 추가
DECLARE
  column_count NUMBER := 0;
BEGIN
  SELECT COUNT(*)
  INTO column_count
  FROM all_tab_columns
  WHERE owner = 'WOORIDO'
    AND table_name = 'USERS'
    AND column_name = 'SOCIAL_PROVIDER';

  IF column_count = 0 THEN
    EXECUTE IMMEDIATE 'ALTER TABLE woorido.users ADD (social_provider VARCHAR2(20))';
  END IF;
END;
/

-- 3) social_id 컬럼 추가
DECLARE
  column_count NUMBER := 0;
BEGIN
  SELECT COUNT(*)
  INTO column_count
  FROM all_tab_columns
  WHERE owner = 'WOORIDO'
    AND table_name = 'USERS'
    AND column_name = 'SOCIAL_ID';

  IF column_count = 0 THEN
    EXECUTE IMMEDIATE 'ALTER TABLE woorido.users ADD (social_id VARCHAR2(100))';
  END IF;
END;
/

-- 4) 소셜 고유 제약 추가
DECLARE
  constraint_count NUMBER := 0;
BEGIN
  SELECT COUNT(*)
  INTO constraint_count
  FROM all_constraints
  WHERE owner = 'WOORIDO'
    AND table_name = 'USERS'
    AND constraint_name = 'UK_SOCIAL_PROVIDER_ID';

  IF constraint_count = 0 THEN
    EXECUTE IMMEDIATE 'ALTER TABLE woorido.users ADD CONSTRAINT uk_social_provider_id UNIQUE (social_provider, social_id)';
  END IF;
END;
/

-- 5) 소셜 조회 인덱스 추가
DECLARE
  index_count NUMBER := 0;
BEGIN
  SELECT COUNT(*)
  INTO index_count
  FROM all_indexes
  WHERE owner = 'WOORIDO'
    AND table_name = 'USERS'
    AND index_name = 'IDX_USERS_SOCIAL';

  IF index_count = 0 THEN
    EXECUTE IMMEDIATE 'CREATE INDEX idx_users_social ON woorido.users(social_provider, social_id)';
  END IF;
END;
/

