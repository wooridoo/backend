-- 1) NOTICE가 아닌 게시글에 남아있는 고정 상태를 해제한다.
UPDATE woorido.posts
SET is_pinned = 'N',
    updated_at = CURRENT_TIMESTAMP
WHERE deleted_at IS NULL
  AND is_pinned = 'Y'
  AND (is_notice IS NULL OR is_notice <> 'Y');

-- 2) 챌린지별로 공지 고정이 여러 건이면 최신 1건만 유지한다.
UPDATE woorido.posts p
SET p.is_pinned = 'N',
    p.updated_at = CURRENT_TIMESTAMP
WHERE p.deleted_at IS NULL
  AND p.is_notice = 'Y'
  AND p.is_pinned = 'Y'
  AND EXISTS (
    SELECT 1
    FROM (
      SELECT id,
             ROW_NUMBER() OVER (
               PARTITION BY challenge_id
               ORDER BY updated_at DESC, created_at DESC, id DESC
             ) AS rn
      FROM woorido.posts
      WHERE deleted_at IS NULL
        AND is_notice = 'Y'
        AND is_pinned = 'Y'
    ) ranked
    WHERE ranked.id = p.id
      AND ranked.rn > 1
  );
