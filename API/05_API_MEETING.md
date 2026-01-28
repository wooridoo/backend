# MEETING API

> **총 API**: 7개
> **담당**: Spring Boot
> **관련 테이블**: `meetings`, `meeting_attendance`

---

## 엔드포인트 목록

| # | Method | Endpoint | 설명 | 우선순위 | 인증 |
|---|--------|----------|------|---------|------|
| 035 | GET | /challenges/{challengeId}/meetings | 모임 목록 조회 | P0 | 멤버 |
| 036 | GET | /meetings/{meetingId} | 모임 상세 조회 | P0 | 멤버 |
| 037 | POST | /challenges/{challengeId}/meetings | 모임 생성 | P0 | 리더 |
| 038 | PUT | /meetings/{meetingId} | 모임 수정 | P1 | 리더 |
| 039 | POST | /meetings/{meetingId}/attendance | 참석 의사 표시 | P0 | 멤버 |
| 040 | POST | /meetings/{meetingId}/complete | 모임 완료 처리 | P0 | 리더 |
| **092** | **DELETE** | **/meetings/{meetingId}/attendance** | **참석 취소** | **P1** | **멤버** |

---

## 035. 모임 목록 조회

### 기본 정보
- **Endpoint**: `GET /challenges/{challengeId}/meetings`
- **우선순위**: P0
- **인증**: 필요 (멤버)

### Request

#### Request Syntax
```bash
curl -X GET "https://api.woorido.com/api/v1/challenges/1/meetings?status=SCHEDULED&page=0" \
  -H "Authorization: Bearer {accessToken}"
```

#### Path Parameters
| 파라미터 | 타입 | 필수 | 설명 |
|----------|------|------|------|
| challengeId | Long | Y | 챌린지 ID |

#### Query Parameters
| 파라미터 | 타입 | 필수 | 기본값 | 설명 |
|----------|------|------|--------|------|
| status | String | N | - | MeetingStatus Enum |
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
        "meetingId": 1,
        "title": "1월 정기모임",
        "description": "신년 계획 공유",
        "status": "COMPLETED",
        "scheduledAt": "2026-01-15T14:00:00Z",
        "location": "강남역 스타벅스",
        "attendance": {
          "confirmed": 8,
          "total": 10
        },
        "beneficiary": {
          "userId": 3,
          "nickname": "박영수"
        },
        "createdAt": "2026-01-01T10:00:00Z"
      }
    ],
    "page": {
      "number": 0,
      "size": 20,
      "totalElements": 12,
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

## 036. 모임 상세 조회

### 기본 정보
- **Endpoint**: `GET /meetings/{meetingId}`
- **우선순위**: P0
- **인증**: 필요 (멤버)

### Request

#### Request Syntax
```bash
curl -X GET https://api.woorido.com/api/v1/meetings/1 \
  -H "Authorization: Bearer {accessToken}"
```

#### Path Parameters
| 파라미터 | 타입 | 필수 | 설명 |
|----------|------|------|------|
| meetingId | Long | Y | 모임 ID |

### Response

#### Response 200 OK
```json
{
  "success": true,
  "data": {
    "meetingId": 1,
    "challengeId": 1,
    "title": "1월 정기모임",
    "description": "신년 계획 공유 및 독서 목표 설정",
    "status": "SCHEDULED",
    "scheduledAt": "2026-01-15T14:00:00Z",
    "location": "강남역 스타벅스",
    "locationDetail": "2층 세미나룸",
    "agenda": "1. 신년 인사\n2. 독서 목표 공유\n3. 다음 모임 일정",
    "attendance": {
      "confirmed": 8,
      "declined": 1,
      "pending": 1,
      "total": 10
    },
    "myAttendance": {
      "status": "CONFIRMED",
      "respondedAt": "2026-01-10T10:00:00Z"
    },
    "beneficiary": {
      "userId": 3,
      "nickname": "박영수",
      "order": 3
    },
    "benefitAmount": 500000,
    "createdBy": {
      "userId": 1,
      "nickname": "홍길동"
    },
    "createdAt": "2026-01-01T10:00:00Z"
  },
  "timestamp": "2026-01-14T10:30:00Z"
}
```

### Errors
| HTTP | 코드 | 메시지 |
|------|------|--------|
| 403 | CHALLENGE_003 | 챌린지 멤버가 아닙니다 |
| 404 | MEETING_001 | 모임을 찾을 수 없습니다 |

---

## 037. 모임 생성 (리더만)

### 기본 정보
- **Endpoint**: `POST /challenges/{challengeId}/meetings`
- **우선순위**: P0
- **인증**: 필요 (리더)

### Request

#### Request Syntax
```bash
curl -X POST https://api.woorido.com/api/v1/challenges/1/meetings \
  -H "Authorization: Bearer {accessToken}" \
  -H "Content-Type: application/json" \
  -d '{
    "title": "2월 정기모임",
    "description": "2월 독서 결산 및 공유",
    "scheduledAt": "2026-02-15T14:00:00Z",
    "location": "강남역 스타벅스",
    "locationDetail": "2층 세미나룸",
    "agenda": "1. 2월 독서 결산\n2. 베스트 도서 선정\n3. 3월 계획"
  }'
```

#### Path Parameters
| 파라미터 | 타입 | 필수 | 설명 |
|----------|------|------|------|
| challengeId | Long | Y | 챌린지 ID |

#### Request Body
| 필드 | 타입 | 필수 | 제약조건 | 설명 |
|------|------|------|----------|------|
| title | String | Y | 최대 100자 | 모임 제목 |
| description | String | N | 최대 500자 | 모임 설명 |
| scheduledAt | String | Y | ISO 8601, 최소 24시간 후 | 예정 일시 |
| location | String | Y | 최대 200자 | 장소 |
| locationDetail | String | N | 최대 200자 | 상세 위치 |
| agenda | String | N | 최대 1000자 | 안건 |

### Response

#### Response 201 Created
```json
{
  "success": true,
  "data": {
    "meetingId": 2,
    "title": "2월 정기모임",
    "status": "SCHEDULED",
    "scheduledAt": "2026-02-15T14:00:00Z",
    "beneficiary": {
      "userId": 4,
      "nickname": "최민수",
      "order": 4
    },
    "createdAt": "2026-01-14T10:30:00Z"
  },
  "message": "모임이 생성되었습니다",
  "timestamp": "2026-01-14T10:30:00Z"
}
```

### 비즈니스 규칙
- 베네핏 수령자는 순번에 따라 자동 지정
- 모임 생성 시 참석 투표가 자동 시작

### Errors
| HTTP | 코드 | 메시지 |
|------|------|--------|
| 400 | MEETING_004 | 예정 일시는 최소 24시간 이후여야 합니다 |
| 403 | CHALLENGE_004 | 리더만 모임을 생성할 수 있습니다 |
| 404 | CHALLENGE_001 | 챌린지를 찾을 수 없습니다 |

---

## 038. 모임 수정 (리더만)

### 기본 정보
- **Endpoint**: `PUT /meetings/{meetingId}`
- **우선순위**: P1
- **인증**: 필요 (리더)

### Request

#### Request Syntax
```bash
curl -X PUT https://api.woorido.com/api/v1/meetings/2 \
  -H "Authorization: Bearer {accessToken}" \
  -H "Content-Type: application/json" \
  -d '{
    "title": "2월 정기모임 (수정)",
    "scheduledAt": "2026-02-16T14:00:00Z"
  }'
```

#### Request Body
| 필드 | 타입 | 필수 | 설명 |
|------|------|------|------|
| title | String | N | 모임 제목 |
| description | String | N | 모임 설명 |
| scheduledAt | String | N | 예정 일시 |
| location | String | N | 장소 |
| locationDetail | String | N | 상세 위치 |
| agenda | String | N | 안건 |

### Response

#### Response 200 OK
```json
{
  "success": true,
  "data": {
    "meetingId": 2,
    "title": "2월 정기모임 (수정)",
    "scheduledAt": "2026-02-16T14:00:00Z",
    "updatedAt": "2026-01-14T10:30:00Z"
  },
  "message": "모임 정보가 수정되었습니다",
  "timestamp": "2026-01-14T10:30:00Z"
}
```

### Errors
| HTTP | 코드 | 메시지 |
|------|------|--------|
| 400 | MEETING_002 | 이미 지난 모임은 수정할 수 없습니다 |
| 403 | CHALLENGE_004 | 리더만 모임을 수정할 수 있습니다 |
| 404 | MEETING_001 | 모임을 찾을 수 없습니다 |

---

## 039. 참석 의사 표시

### 기본 정보
- **Endpoint**: `POST /meetings/{meetingId}/attendance`
- **우선순위**: P0
- **인증**: 필요 (멤버)

### Request

#### Request Syntax
```bash
curl -X POST https://api.woorido.com/api/v1/meetings/2/attendance \
  -H "Authorization: Bearer {accessToken}" \
  -H "Content-Type: application/json" \
  -d '{
    "status": "CONFIRMED"
  }'
```

#### Request Body
| 필드 | 타입 | 필수 | 제약조건 | 설명 |
|------|------|------|----------|------|
| status | String | Y | CONFIRMED / DECLINED | 참석 여부 |
| reason | String | N | 최대 200자 | 불참 사유 (불참 시) |

### Response

#### Response 200 OK
```json
{
  "success": true,
  "data": {
    "meetingId": 2,
    "myAttendance": {
      "status": "CONFIRMED",
      "respondedAt": "2026-01-14T10:30:00Z"
    },
    "attendance": {
      "confirmed": 9,
      "declined": 0,
      "pending": 1,
      "total": 10
    }
  },
  "message": "참석 의사가 등록되었습니다",
  "timestamp": "2026-01-14T10:30:00Z"
}
```

### Errors
| HTTP | 코드 | 메시지 |
|------|------|--------|
| 400 | MEETING_002 | 이미 지난 모임입니다 |
| 400 | MEETING_003 | 이미 참석 의사를 표시했습니다 |
| 404 | MEETING_001 | 모임을 찾을 수 없습니다 |

---

## 040. 모임 완료 처리 (리더만)

### 기본 정보
- **Endpoint**: `POST /meetings/{meetingId}/complete`
- **우선순위**: P0
- **인증**: 필요 (리더)

### Request

#### Request Syntax
```bash
curl -X POST https://api.woorido.com/api/v1/meetings/1/complete \
  -H "Authorization: Bearer {accessToken}" \
  -H "Content-Type: application/json" \
  -d '{
    "actualAttendees": [1, 2, 3, 4, 5, 6, 7, 8],
    "notes": "성공적인 모임이었습니다."
  }'
```

#### Request Body
| 필드 | 타입 | 필수 | 설명 |
|------|------|------|------|
| actualAttendees | Long[] | Y | 실제 참석자 userId 배열 |
| notes | String | N | 모임 메모 |

### Response

#### Response 200 OK
```json
{
  "success": true,
  "data": {
    "meetingId": 1,
    "status": "COMPLETED",
    "attendance": {
      "actual": 8,
      "total": 10,
      "rate": 80.0
    },
    "benefit": {
      "beneficiary": {
        "userId": 3,
        "nickname": "박영수"
      },
      "amount": 500000,
      "transferredAt": "2026-01-14T10:30:00Z"
    },
    "completedAt": "2026-01-14T10:30:00Z"
  },
  "message": "모임이 완료 처리되었습니다",
  "timestamp": "2026-01-14T10:30:00Z"
}
```

### Errors
| HTTP | 코드 | 메시지 |
|------|------|--------|
| 400 | MEETING_005 | 이미 완료된 모임입니다 |
| 400 | ACCOUNT_004 | 챌린지 잔액이 부족합니다 |
| 403 | CHALLENGE_004 | 리더만 완료 처리할 수 있습니다 |
| 404 | MEETING_001 | 모임을 찾을 수 없습니다 |

---

## 092. 참석 취소 ⭐ NEW

### 기본 정보
- **Endpoint**: `DELETE /meetings/{meetingId}/attendance`
- **우선순위**: P1
- **인증**: 필요 (멤버)

### Request

#### Request Syntax
```bash
curl -X DELETE https://api.woorido.com/api/v1/meetings/2/attendance \
  -H "Authorization: Bearer {accessToken}"
```

#### Path Parameters
| 파라미터 | 타입 | 필수 | 설명 |
|----------|------|------|------|
| meetingId | Long | Y | 모임 ID |

### Response

#### Response 200 OK
```json
{
  "success": true,
  "data": {
    "meetingId": 2,
    "myAttendance": null,
    "attendance": {
      "confirmed": 8,
      "declined": 0,
      "pending": 2,
      "total": 10
    },
    "cancelledAt": "2026-01-14T10:30:00Z"
  },
  "message": "참석 의사가 취소되었습니다",
  "timestamp": "2026-01-14T10:30:00Z"
}
```

### 비즈니스 규칙
- 참석 의사를 표시한 경우에만 취소 가능
- 취소 후 상태는 PENDING으로 변경

### Errors
| HTTP | 코드 | 메시지 |
|------|------|--------|
| 400 | MEETING_002 | 이미 지난 모임입니다 |
| 400 | MEETING_006 | 참석 의사를 표시하지 않았습니다 |
| 404 | MEETING_001 | 모임을 찾을 수 없습니다 |

---

**관련 문서:**
- [00_API_OVERVIEW.md](./00_API_OVERVIEW.md) - 공통 사항
- [04_API_CHALLENGE.md](./04_API_CHALLENGE.md) - 챌린지 API
- [06_API_VOTE.md](./06_API_VOTE.md) - 투표 API
