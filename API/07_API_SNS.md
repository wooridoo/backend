# SNS API

> **총 API**: 17개 (게시물 9개 + 댓글 6개 + 지출 2개)
> **담당**: Spring Boot
> **관련 테이블**: `posts`, `comments`, `post_likes`, `expenses`, `ledger_entries`

---

## 엔드포인트 목록

### 게시글 (POST)

| # | Method | Endpoint | 설명 | 우선순위 | 인증 |
|---|--------|----------|------|---------|------|
| 057 | GET | /challenges/{challengeId}/posts | 게시글 목록 조회 | P0 | 멤버 |
| 058 | GET | /challenges/{challengeId}/posts/{postId} | 게시글 상세 조회 | P0 | 멤버 |
| 059 | POST | /challenges/{challengeId}/posts | 게시글 작성 | P0 | 멤버 |
| 060 | PUT | /challenges/{challengeId}/posts/{postId} | 게시글 수정 | P1 | 작성자 |
| 061 | DELETE | /challenges/{challengeId}/posts/{postId} | 게시글 삭제 | P1 | 작성자/리더 |
| 062 | POST | /challenges/{challengeId}/posts/{postId}/like | 게시글 좋아요 | P1 | 멤버 |
| 063 | PUT | /challenges/{challengeId}/posts/{postId}/pin | 게시글 상단 고정 | P2 | 리더 |
| 064 | POST | /challenges/{challengeId}/posts/upload | 파일 업로드 | P1 | 멤버 |
| 065 | GET | /posts/my | 내 게시글 목록 조회 | P2 | 필요 |

### 댓글 (COMMENT)

| # | Method | Endpoint | 설명 | 우선순위 | 인증 |
|---|--------|----------|------|---------|------|
| 066 | GET | /challenges/{challengeId}/posts/{postId}/comments | 댓글 목록 조회 | P0 | 멤버 |
| 067 | POST | /challenges/{challengeId}/posts/{postId}/comments | 댓글 작성 | P0 | 멤버 |
| 068 | PUT | /comments/{commentId} | 댓글 수정 | P1 | 작성자 |
| 069 | DELETE | /comments/{commentId} | 댓글 삭제 | P1 | 작성자/리더 |
| 070 | POST | /comments/{commentId}/like | 댓글 좋아요 | P2 | 멤버 |
| 071 | POST | /comments/{commentId}/replies | 대댓글 작성 | P1 | 멤버 |

### 지출/장부 (EXPENSE/LEDGER)

| # | Method | Endpoint | 설명 | 우선순위 | 인증 |
|---|--------|----------|------|---------|------|
| 046-051 | - | /challenges/{challengeId}/expenses/* | 지출 API (별도) | P1 | 멤버/리더 |
| 052-053 | - | /challenges/{challengeId}/ledger/* | 장부 API (별도) | P1 | 멤버/리더 |

---

## 057. 게시글 목록 조회

### 기본 정보
- **Endpoint**: `GET /challenges/{challengeId}/posts`
- **우선순위**: P0
- **인증**: 필요 (멤버)

### Request

#### Request Syntax
```bash
curl -X GET "https://api.woorido.com/api/v1/challenges/1/posts?category=NOTICE&page=0&size=20" \
  -H "Authorization: Bearer {accessToken}"
```

#### Query Parameters
| 파라미터 | 타입 | 필수 | 기본값 | 설명 |
|----------|------|------|--------|------|
| page | Integer | N | 0 | 페이지 번호 |
| size | Integer | N | 20 | 페이지 크기 |
| category | String | N | - | ALL / NOTICE / GENERAL / QUESTION |
| sortBy | String | N | - | CREATED_AT / LIKES / COMMENTS |
| order | String | N | DESC | DESC / ASC |

### Response

#### Response 200 OK
```json
{
  "success": true,
  "data": {
    "content": [
      {
        "postId": 1,
        "title": "[공지] 2월 정기모임 안내",
        "content": "2월 정기모임은 2월 15일 토요일 오후 2시에...",
        "category": "NOTICE",
        "author": {
          "userId": 1,
          "nickname": "홍길동",
          "profileImage": "https://cdn.woorido.com/profiles/1.jpg"
        },
        "likeCount": 8,
        "commentCount": 5,
        "viewCount": 45,
        "isPinned": true,
        "createdAt": "2026-01-10T09:00:00Z"
      }
    ],
    "totalElements": 23,
    "totalPages": 2,
    "number": 0,
    "size": 20
  },
  "timestamp": "2026-01-14T10:30:00Z"
}
```

### Errors
| HTTP | 코드 | 메시지 |
|------|------|--------|
| 403 | MEMBER_001 | 챌린지에 참여하지 않았습니다 |
| 404 | CHALLENGE_001 | 챌린지를 찾을 수 없습니다 |

---

## 058. 게시글 상세 조회

### 기본 정보
- **Endpoint**: `GET /challenges/{challengeId}/posts/{postId}`
- **우선순위**: P0
- **인증**: 필요 (멤버)

### Response

#### Response 200 OK
```json
{
  "success": true,
  "data": {
    "postId": 1,
    "title": "[공지] 2월 정기모임 안내",
    "content": "2월 정기모임은 2월 15일 토요일 오후 2시에 강남역 스터디카페에서 진행됩니다.\n\n참석 여부를 댓글로 알려주세요!",
    "category": "NOTICE",
    "author": {
      "userId": 1,
      "nickname": "홍길동",
      "profileImage": "https://cdn.woorido.com/profiles/1.jpg"
    },
    "attachments": [
      {
        "fileId": 1,
        "fileName": "모임장소_안내.jpg",
        "fileUrl": "https://cdn.woorido.com/attachments/1.jpg",
        "fileSize": 245000
      }
    ],
    "likeCount": 8,
    "commentCount": 5,
    "viewCount": 46,
    "isLiked": false,
    "isPinned": true,
    "createdAt": "2026-01-10T09:00:00Z",
    "updatedAt": "2026-01-10T09:00:00Z"
  },
  "timestamp": "2026-01-14T10:30:00Z"
}
```

---

## 059. 게시글 작성

### 기본 정보
- **Endpoint**: `POST /challenges/{challengeId}/posts`
- **우선순위**: P0
- **인증**: 필요 (멤버)

### Request

#### Request Syntax
```bash
curl -X POST https://api.woorido.com/api/v1/challenges/1/posts \
  -H "Authorization: Bearer {accessToken}" \
  -H "Content-Type: application/json" \
  -d '{
    "title": "저축 꿀팁 공유합니다!",
    "content": "제가 사용하는 저축 방법을 공유드려요.\n\n1. 월급 받으면 바로 저축\n2. 고정 지출 먼저 빼기\n3. 남은 금액으로 생활하기",
    "category": "GENERAL",
    "attachmentIds": []
  }'
```

#### Request Body
| 필드 | 타입 | 필수 | 제약조건 | 설명 |
|------|------|------|----------|------|
| title | String | Y | 2~100자 | 게시글 제목 |
| content | String | Y | 1~10000자 | 게시글 내용 |
| category | String | Y | NOTICE/GENERAL/QUESTION | 카테고리 |
| attachmentIds | Array | N | 최대 10개 | 첨부파일 ID 목록 |

### Response

#### Response 201 Created
```json
{
  "success": true,
  "data": {
    "postId": 24,
    "title": "저축 꿀팁 공유합니다!",
    "category": "GENERAL",
    "author": {
      "userId": 3,
      "nickname": "박저축"
    },
    "createdAt": "2026-01-14T10:30:00Z"
  },
  "message": "게시글이 작성되었습니다",
  "timestamp": "2026-01-14T10:30:00Z"
}
```

### Errors
| HTTP | 코드 | 메시지 |
|------|------|--------|
| 400 | POST_003 | 유효하지 않은 첨부파일입니다 |
| 403 | MEMBER_001 | 챌린지에 참여하지 않았습니다 |
| 403 | POST_002 | 공지사항은 모임장만 작성할 수 있습니다 |

---

## 062. 게시글 좋아요

### 기본 정보
- **Endpoint**: `POST /challenges/{challengeId}/posts/{postId}/like`
- **우선순위**: P1
- **인증**: 필요 (멤버)

### Response

#### Response 200 OK
```json
{
  "success": true,
  "data": {
    "postId": 24,
    "liked": true,
    "likeCount": 5
  },
  "message": "좋아요를 눌렀습니다",
  "timestamp": "2026-01-14T10:30:00Z"
}
```

### 비즈니스 로직
- 이미 좋아요한 경우 토글 (좋아요 취소)
- 취소 시 `liked: false`, 메시지: "좋아요를 취소했습니다"

---

## 064. 파일 업로드

### 기본 정보
- **Endpoint**: `POST /challenges/{challengeId}/posts/upload`
- **우선순위**: P1
- **인증**: 필요 (멤버)

### Request

#### Request Syntax
```bash
curl -X POST https://api.woorido.com/api/v1/challenges/1/posts/upload \
  -H "Authorization: Bearer {accessToken}" \
  -F "file=@저축계획표.xlsx"
```

#### 지원 형식
- **이미지**: jpg, jpeg, png, gif, webp
- **문서**: pdf, doc, docx, xls, xlsx, ppt, pptx
- **크기 제한**: 최대 10MB

### Response

#### Response 200 OK
```json
{
  "success": true,
  "data": {
    "fileId": 15,
    "fileName": "저축계획표.xlsx",
    "fileUrl": "https://cdn.woorido.com/attachments/15.xlsx",
    "fileSize": 25600,
    "contentType": "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
  },
  "message": "파일이 업로드되었습니다",
  "timestamp": "2026-01-14T10:30:00Z"
}
```

### Errors
| HTTP | 코드 | 메시지 |
|------|------|--------|
| 400 | POST_006 | 파일 크기가 10MB를 초과합니다 |
| 400 | POST_007 | 지원하지 않는 파일 형식입니다 |

---

## 066. 댓글 목록 조회

### 기본 정보
- **Endpoint**: `GET /challenges/{challengeId}/posts/{postId}/comments`
- **우선순위**: P0
- **인증**: 필요 (멤버)

### Response

#### Response 200 OK
```json
{
  "success": true,
  "data": {
    "comments": [
      {
        "commentId": 1,
        "content": "좋은 정보 감사합니다!",
        "author": {
          "userId": 2,
          "nickname": "김저축",
          "profileImage": "https://cdn.woorido.com/profiles/2.jpg"
        },
        "likeCount": 3,
        "isLiked": false,
        "replies": [
          {
            "commentId": 5,
            "content": "@김저축 도움이 되셨다니 기쁘네요!",
            "author": {
              "userId": 3,
              "nickname": "박저축"
            },
            "createdAt": "2026-01-14T11:30:00Z"
          }
        ],
        "createdAt": "2026-01-14T11:00:00Z"
      }
    ],
    "totalCount": 5
  },
  "timestamp": "2026-01-14T10:30:00Z"
}
```

---

## 067. 댓글 작성

### 기본 정보
- **Endpoint**: `POST /challenges/{challengeId}/posts/{postId}/comments`
- **우선순위**: P0
- **인증**: 필요 (멤버)

### Request

#### Request Syntax
```bash
curl -X POST https://api.woorido.com/api/v1/challenges/1/posts/1/comments \
  -H "Authorization: Bearer {accessToken}" \
  -H "Content-Type: application/json" \
  -d '{
    "content": "좋은 정보 감사합니다!"
  }'
```

#### Request Body
| 필드 | 타입 | 필수 | 제약조건 | 설명 |
|------|------|------|----------|------|
| content | String | Y | 1~1000자 | 댓글 내용 |

### Response

#### Response 201 Created
```json
{
  "success": true,
  "data": {
    "commentId": 6,
    "content": "좋은 정보 감사합니다!",
    "author": {
      "userId": 4,
      "nickname": "이저축"
    },
    "createdAt": "2026-01-14T12:00:00Z"
  },
  "message": "댓글이 작성되었습니다",
  "timestamp": "2026-01-14T12:00:00Z"
}
```

---

## 071. 대댓글 작성

### 기본 정보
- **Endpoint**: `POST /comments/{commentId}/replies`
- **우선순위**: P1
- **인증**: 필요 (멤버)

### Request

#### Request Syntax
```bash
curl -X POST https://api.woorido.com/api/v1/comments/1/replies \
  -H "Authorization: Bearer {accessToken}" \
  -H "Content-Type: application/json" \
  -d '{
    "content": "@김저축 도움이 되셨다니 기쁘네요!"
  }'
```

### Response

#### Response 201 Created
```json
{
  "success": true,
  "data": {
    "commentId": 7,
    "parentId": 1,
    "content": "@김저축 도움이 되셨다니 기쁘네요!",
    "author": {
      "userId": 3,
      "nickname": "박저축"
    },
    "createdAt": "2026-01-14T12:30:00Z"
  },
  "message": "답글이 작성되었습니다",
  "timestamp": "2026-01-14T12:30:00Z"
}
```

---

**관련 문서:**
- [00_API_OVERVIEW.md](./00_API_OVERVIEW.md) - 공통 사항
- [06_API_VOTE.md](./06_API_VOTE.md) - 투표 API
- [08_API_SYSTEM.md](./08_API_SYSTEM.md) - 시스템 API
