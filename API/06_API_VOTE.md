# VOTE API

> **총 API**: 5개
> **담당**: Spring Boot
> **관련 테이블**: `votes`, `vote_records`

---

## 엔드포인트 목록

| # | Method | Endpoint | 설명 | 우선순위 | 인증 |
|---|--------|----------|------|---------|------|
| 041 | GET | /challenges/{challengeId}/votes | 투표 목록 조회 | P0 | 멤버 |
| 042 | GET | /votes/{voteId} | 투표 상세 조회 | P0 | 멤버 |
| 043 | POST | /challenges/{challengeId}/votes | 투표 생성 | P0 | 멤버 |
| 044 | PUT | /votes/{voteId}/cast | 투표하기 | P0 | 멤버 |
| 045 | GET | /votes/{voteId}/result | 투표 결과 조회 | P1 | 멤버 |

---

## 041. 투표 목록 조회

### 기본 정보
- **Endpoint**: `GET /challenges/{challengeId}/votes`
- **우선순위**: P0
- **인증**: 필요 (멤버)

### Request

#### Request Syntax
```bash
curl -X GET "https://api.woorido.com/api/v1/challenges/1/votes?status=IN_PROGRESS&page=0" \
  -H "Authorization: Bearer {accessToken}"
```

#### Query Parameters
| 파라미터 | 타입 | 필수 | 기본값 | 설명 |
|----------|------|------|--------|------|
| status | String | N | - | VoteStatus Enum |
| type | String | N | - | VoteType Enum |
| page | Integer | N | 0 | 페이지 번호 |
| size | Integer | N | 20 | 페이지 크기 |

### Response

#### Response 200 OK
```json
{
  "success": true,
  "data": {
    "content": [
      {
        "voteId": 1,
        "type": "EXPENSE",
        "title": "회식비 30만원 지출 승인",
        "status": "IN_PROGRESS",
        "createdBy": {
          "userId": 1,
          "nickname": "홍길동"
        },
        "voteCount": {
          "agree": 5,
          "disagree": 2,
          "total": 10
        },
        "deadline": "2026-01-15T23:59:59Z",
        "createdAt": "2026-01-14T10:00:00Z"
      }
    ],
    "page": {
      "number": 0,
      "size": 20,
      "totalElements": 1,
      "totalPages": 1
    }
  },
  "timestamp": "2026-01-14T10:30:00Z"
}
```

### Errors
| HTTP | 코드 | 메시지 |
|------|------|--------|
| 403 | CHALLENGE_003 | 챌린지 멤버가 아닙니다 |
| 404 | CHALLENGE_001 | 챌린지를 찾을 수 없습니다 |

---

## 042. 투표 상세 조회

### 기본 정보
- **Endpoint**: `GET /votes/{voteId}`
- **우선순위**: P0
- **인증**: 필요 (멤버)

### Request

#### Request Syntax
```bash
curl -X GET https://api.woorido.com/api/v1/votes/1 \
  -H "Authorization: Bearer {accessToken}"
```

### Response

#### Response 200 OK
```json
{
  "success": true,
  "data": {
    "voteId": 1,
    "challengeId": 1,
    "type": "EXPENSE",
    "title": "회식비 30만원 지출 승인",
    "description": "1월 정기모임 회식비입니다",
    "status": "IN_PROGRESS",
    "createdBy": {
      "userId": 1,
      "nickname": "홍길동",
      "profileImage": "https://cdn.woorido.com/profiles/1.jpg"
    },
    "targetInfo": {
      "expenseId": 5,
      "amount": 300000,
      "category": "FOOD"
    },
    "voteCount": {
      "agree": 5,
      "disagree": 2,
      "abstain": 1,
      "notVoted": 2,
      "total": 10
    },
    "myVote": "AGREE",
    "eligibleVoters": 10,
    "requiredApproval": 7,
    "deadline": "2026-01-15T23:59:59Z",
    "createdAt": "2026-01-14T10:00:00Z"
  },
  "timestamp": "2026-01-14T10:30:00Z"
}
```

#### Response Elements
| 필드 | 타입 | 설명 |
|------|------|------|
| data.type | String | 투표 유형 (VoteType) |
| data.status | String | 상태 (VoteStatus) |
| data.targetInfo | Object | 대상 정보 (지출/멤버 등) |
| data.myVote | String | 내 투표 선택 (미투표 시 null) |
| data.requiredApproval | Integer | 승인 필요 인원 (70%) |

### Errors
| HTTP | 코드 | 메시지 |
|------|------|--------|
| 403 | VOTE_003 | 투표 조회 권한이 없습니다 |
| 404 | VOTE_001 | 투표를 찾을 수 없습니다 |

---

## 043. 투표 생성

### 기본 정보
- **Endpoint**: `POST /challenges/{challengeId}/votes`
- **우선순위**: P0
- **인증**: 필요 (멤버)

### Request

#### Request Syntax
```bash
curl -X POST https://api.woorido.com/api/v1/challenges/1/votes \
  -H "Authorization: Bearer {accessToken}" \
  -H "Content-Type: application/json" \
  -d '{
    "type": "EXPENSE",
    "title": "회식비 30만원 지출 승인",
    "description": "1월 정기모임 회식비입니다",
    "targetId": 5,
    "deadline": "2026-01-15T23:59:59Z"
  }'
```

#### Request Body
| 필드 | 타입 | 필수 | 제약조건 | 설명 |
|------|------|------|----------|------|
| type | String | Y | VoteType Enum | 투표 유형 |
| title | String | Y | 최대 100자 | 투표 제목 |
| description | String | N | 최대 500자 | 투표 설명 |
| targetId | Long | N | - | 대상 ID (지출ID/멤버ID) |
| meetingId | Long | N | - | 모임 ID (지출 승인 시 모임 연동) |
| deadline | String | Y | ISO 8601, 최소 24시간 후 | 마감 시간 |

#### VoteType Enum
| 타입 | 용도 | targetId |
|------|------|----------|
| EXPENSE | 지출 승인 투표 | expenseId |
| KICK | 멤버 강퇴 투표 | memberId |
| LEADER_KICK | 리더 강퇴 투표 | leaderId (팔로워만 투표) |
| DISSOLVE | 챌린지 해산 투표 | 없음 |
| MEETING_ATTENDANCE | 모임 참석/개최 투표 (P-039) | meetingId |

### 비즈니스 규칙
**승인 조건 (Approval Thresholds)**
- **EXPENSE (지출)**: 투표 참여자의 과반수 (50% + 1)
- **MEETING_ATTENDANCE (모임 개최)**: 전체 멤버의 과반수 (50% + 1)
- **KICK / LEADER_KICK (강퇴)**: 전체 멤버(대상자 제외)의 70% 이상
- **DISSOLVE (해산)**: 전체 멤버의 100% (전원 동의)
- **MEETING_EXPENSE (모임 지출)**: 모임 참석자의 과반수 (P-042)

**권한 규칙 (Permission Rules)**
- **EXPENSE / DISSOLVE / MEETING_ATTENDANCE**: 리더만 생성 가능 (PM-007, PM-009)
- **LEADER_KICK**: 팔로워만 생성 가능 (30일 미활동 시, PM-020)
- **KICK**: 모든 멤버 생성 가능 (대상자 제외, PM-014)

### Response

#### Response 201 Created
```json
{
  "success": true,
  "data": {
    "voteId": 1,
    "type": "EXPENSE",
    "title": "회식비 30만원 지출 승인",
    "status": "IN_PROGRESS",
    "deadline": "2026-01-15T23:59:59Z",
    "createdAt": "2026-01-14T10:30:00Z"
  },
  "message": "투표가 생성되었습니다",
  "timestamp": "2026-01-14T10:30:00Z"
}
```

### Errors
| HTTP | 코드 | 메시지 |
|------|------|--------|
| 400 | VOTE_002 | 마감 시간은 최소 24시간 이후여야 합니다 |
| 403 | CHALLENGE_003 | 챌린지 멤버가 아닙니다 |
| 404 | CHALLENGE_001 | 챌린지를 찾을 수 없습니다 |
| 409 | VOTE_004 | 이미 진행 중인 동일 유형의 투표가 있습니다 |

---

## 044. 투표하기

### 기본 정보
- **Endpoint**: `PUT /votes/{voteId}/cast`
- **우선순위**: P0
- **인증**: 필요 (멤버)

### Request

#### Request Syntax
```bash
curl -X PUT https://api.woorido.com/api/v1/votes/1/cast \
  -H "Authorization: Bearer {accessToken}" \
  -H "Content-Type: application/json" \
  -d '{
    "choice": "AGREE"
  }'
```

#### Request Body
| 필드 | 타입 | 필수 | 제약조건 | 설명 |
|------|------|------|----------|------|
| choice | String | Y | AGREE / DISAGREE / ABSTAIN | 투표 선택 |

### Response

#### Response 200 OK
```json
{
  "success": true,
  "data": {
    "voteId": 1,
    "myVote": "AGREE",
    "voteCount": {
      "agree": 6,
      "disagree": 2,
      "abstain": 1,
      "notVoted": 1,
      "total": 10
    },
    "votedAt": "2026-01-14T10:30:00Z"
  },
  "message": "투표가 완료되었습니다",
  "timestamp": "2026-01-14T10:30:00Z"
}
```

### Errors
| HTTP | 코드 | 메시지 |
|------|------|--------|
| 400 | VOTE_005 | 투표가 마감되었습니다 |
| 403 | VOTE_003 | 투표 권한이 없습니다 |
| 403 | MEMBER_003 | 정지된 멤버는 투표할 수 없습니다 |
| 404 | VOTE_001 | 투표를 찾을 수 없습니다 |
| 409 | VOTE_006 | 이미 투표하셨습니다 |

---

## 045. 투표 결과 조회

### 기본 정보
- **Endpoint**: `GET /votes/{voteId}/result`
- **우선순위**: P1
- **인증**: 필요 (멤버)

### Request

#### Request Syntax
```bash
curl -X GET https://api.woorido.com/api/v1/votes/1/result \
  -H "Authorization: Bearer {accessToken}"
```

### Response

#### Response 200 OK
```json
{
  "success": true,
  "data": {
    "voteId": 1,
    "type": "EXPENSE",
    "title": "회식비 30만원 지출 승인",
    "status": "APPROVED",
    "result": {
      "passed": true,
      "agree": 8,
      "disagree": 1,
      "abstain": 1,
      "notVoted": 0,
      "total": 10,
      "requiredApproval": 7,
      "approvalRate": 80.0
    },
    "voters": [
      {
        "userId": 1,
        "nickname": "홍길동",
        "choice": "AGREE",
        "votedAt": "2026-01-14T11:00:00Z"
      }
    ],
    "completedAt": "2026-01-15T23:59:59Z"
  },
  "timestamp": "2026-01-14T10:30:00Z"
}
```

### Errors
| HTTP | 코드 | 메시지 |
|------|------|--------|
| 400 | VOTE_007 | 아직 진행 중인 투표입니다 |
| 403 | VOTE_003 | 투표 조회 권한이 없습니다 |
| 404 | VOTE_001 | 투표를 찾을 수 없습니다 |

---

**관련 문서:**
- [00_API_OVERVIEW.md](./00_API_OVERVIEW.md) - 공통 사항
- [05_API_MEETING.md](./05_API_MEETING.md) - 모임 API
- [07_API_SNS.md](./07_API_SNS.md) - SNS API
