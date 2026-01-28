# CHALLENGE API

> **총 API**: 13개 (챌린지 8개 + 멤버 5개)
> **담당**: Spring Boot
> **관련 테이블**: `challenges`, `challenge_members`, `ledger_entries`

---

## 엔드포인트 목록

### 챌린지

| # | Method | Endpoint | 설명 | 우선순위 | 인증 |
|---|--------|----------|------|---------|------|
| 022 | POST | /challenges | 챌린지 생성 | P0 | 필요 |
| 023 | GET | /challenges | 챌린지 목록 조회 | P0 | 필요 |
| 024 | GET | /challenges/{challengeId} | 챌린지 상세 조회 | P0 | 필요 |
| 025 | PUT | /challenges/{challengeId} | 챌린지 수정 | P0 | 리더 |
| 026 | DELETE | /challenges/{challengeId} | 챌린지 삭제 | P1 | 리더 |
| 027 | GET | /challenges/me | 내 챌린지 목록 | P0 | 필요 |
| 028 | GET | /challenges/{challengeId}/account | 챌린지 어카운트 조회 | P0 | 멤버 |
| 029 | PUT | /challenges/{challengeId}/support/settings | 자동 납입 설정 | P1 | 멤버 |

### 챌린지 멤버

| # | Method | Endpoint | 설명 | 우선순위 | 인증 |
|---|--------|----------|------|---------|------|
| 030 | POST | /challenges/{challengeId}/join | 챌린지 가입 | P0 | 필요 |
| 031 | DELETE | /challenges/{challengeId}/leave | 챌린지 탈퇴 | P0 | 필요 |
| 032 | GET | /challenges/{challengeId}/members | 멤버 목록 조회 | P0 | 멤버 |
| 033 | GET | /challenges/{challengeId}/members/{memberId} | 멤버 상세 조회 | P1 | 멤버 |
| 034 | POST | /challenges/{challengeId}/delegate | 리더 위임 | P1 | 리더 |

---

## 022. 챌린지 생성

### 기본 정보
- **Endpoint**: `POST /challenges`
- **우선순위**: P0
- **인증**: 필요

### Request

#### Request Syntax
```bash
curl -X POST https://api.woorido.com/api/v1/challenges \
  -H "Authorization: Bearer {accessToken}" \
  -H "Content-Type: application/json" \
  -d '{
    "name": "책벌레들",
    "description": "매월 책 1권 완독하는 독서 모임입니다",
    "category": "HOBBY",
    "maxMembers": 10,
    "supportAmount": 100000,
    "depositAmount": 200000,
    "supportDay": 1,
    "startDate": "2026-02-01",
    "thumbnailImage": "https://cdn.woorido.com/challenges/book.jpg",
    "rules": "1. 매월 1권 이상 완독\n2. 독서 인증 필수"
  }'
```

#### Request Body
| 필드 | 타입 | 필수 | 제약조건 | 설명 |
|------|------|------|----------|------|
| name | String | Y | 2-50자 | 챌린지 이름 |
| description | String | Y | 최대 500자 | 챌린지 설명 |
| category | String | Y | ChallengeCategory Enum | 카테고리 |
| maxMembers | Integer | Y | 3-30명 | 최대 인원 |
| supportAmount | Long | Y | 10,000원 이상, 10,000원 단위 | 월 서포트 금액 |
| depositAmount | Long | Y | 서포트 금액의 1-3배 | 보증금 |
| supportDay | Integer | Y | 1-28 | 월 납입일 |
| startDate | String | Y | YYYY-MM-DD, 최소 7일 후 | 시작 예정일 |
| thumbnailImage | String | N | URL 형식 | 썸네일 이미지 |
| rules | String | N | 최대 1000자 | 챌린지 규칙 |

#### ChallengeCategory Enum
| 값 | 설명 |
|----|
| HOBBY | 취미 |
| STUDY | 학습 |
| EXERCISE | 운동 |
| SAVINGS | 저축 |
| TRAVEL | 여행 |
| FOOD | 음식 |
| CULTURE | 문화 |
| OTHER | 기타 |

### Response

#### Response 201 Created
```json
{
  "success": true,
  "data": {
    "challengeId": 1,
    "name": "책벌레들",
    "status": "RECRUITING",
    "memberCount": {
      "current": 1,
      "max": 10
    },
    "myRole": "LEADER",
    "createdAt": "2026-01-14T10:30:00Z"
  },
  "message": "챌린지가 생성되었습니다",
  "timestamp": "2026-01-14T10:30:00Z"
}
```

### 비즈니스 규칙
- 생성자는 자동으로 리더가 됨
- 리더당 최대 3개 챌린지 생성 가능

### Errors
| HTTP | 코드 | 메시지 |
|------|------|--------|
| 400 | CHALLENGE_007 | 리더당 챌린지 생성 한도(3개)를 초과했습니다 |
| 400 | VALIDATION_001 | 유효성 검증 실패 |

---

## 023. 챌린지 목록 조회

### 기본 정보
- **Endpoint**: `GET /challenges`
- **우선순위**: P0
- **인증**: 필요

### Request

#### Request Syntax
```bash
curl -X GET "https://api.woorido.com/api/v1/challenges?status=RECRUITING&page=0&size=20" \
  -H "Authorization: Bearer {accessToken}"
```

#### Query Parameters
| 파라미터 | 타입 | 필수 | 기본값 | 설명 |
|----------|------|------|--------|------|
| status | String | N | - | RECRUITING / IN_PROGRESS / COMPLETED |
| category | String | N | - | 카테고리 필터 |
| sort | String | N | createdAt,desc | 정렬 기준 |
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
        "challengeId": 1,
        "name": "책벌레들",
        "description": "매월 책 1권 완독하는 독서 모임",
        "category": "HOBBY",
        "status": "RECRUITING",
        "memberCount": {
          "current": 8,
          "max": 10
        },
        "supportAmount": 100000,
        "thumbnailImage": "https://cdn.woorido.com/challenges/1.jpg",
        "isVerified": true,
        "leader": {
          "userId": 1,
          "nickname": "홍길동"
        },
        "createdAt": "2025-12-01T10:00:00Z"
      }
    ],
    "page": {
      "number": 0,
      "size": 20,
      "totalElements": 50,
      "totalPages": 3
    }
  },
  "timestamp": "2026-01-14T10:30:00Z"
}
```

---

## 024. 챌린지 상세 조회

### 기본 정보
- **Endpoint**: `GET /challenges/{challengeId}`
- **우선순위**: P0
- **인증**: 필요

### Request

#### Request Syntax
```bash
curl -X GET https://api.woorido.com/api/v1/challenges/1 \
  -H "Authorization: Bearer {accessToken}"
```

#### Path Parameters
| 파라미터 | 타입 | 필수 | 설명 |
|----------|------|------|------|
| challengeId | Long | Y | 챌린지 ID |

### Response

#### Response 200 OK
```json
{
  "success": true,
  "data": {
    "challengeId": 1,
    "name": "책벌레들",
    "description": "매월 책 1권 완독하는 독서 모임입니다",
    "category": "HOBBY",
    "status": "ACTIVE",
    "memberCount": {
      "current": 10,
      "max": 10
    },
    "supportAmount": 100000,
    "depositAmount": 200000,
    "supportDay": 1,
    "thumbnailImage": "https://cdn.woorido.com/challenges/1.jpg",
    "rules": "1. 매월 1권 이상 완독\n2. 독서 인증 필수",
    "isVerified": true,
    "leader": {
      "userId": 1,
      "nickname": "홍길동",
      "profileImage": "https://cdn.woorido.com/profiles/1.jpg"
    },
    "account": {
      "balance": 5000000,
      "totalSupport": 10000000,
      "totalExpense": 5000000
    },
    "stats": {
      "totalMeetings": 12,
      "completedMeetings": 10,
      "averageAttendance": 85.0
    },
    "isMember": true,
    "myMembership": {
      "memberId": 5,
      "role": "FOLLOWER",
      "status": "ACTIVE",
      "joinedAt": "2025-12-15T10:00:00Z"
    },
    "startedAt": "2026-01-01T00:00:00Z",
    "createdAt": "2025-12-01T10:00:00Z"
  },
  "timestamp": "2026-01-14T10:30:00Z"
}
```

### Errors
| HTTP | 코드 | 메시지 |
|------|------|--------|
| 404 | CHALLENGE_001 | 챌린지를 찾을 수 없습니다 |

---

## 025. 챌린지 수정 (리더만)

### 기본 정보
- **Endpoint**: `PUT /challenges/{challengeId}`
- **우선순위**: P0
- **인증**: 필요 (리더)

### Request

#### Request Syntax
```bash
curl -X PUT https://api.woorido.com/api/v1/challenges/1 \
  -H "Authorization: Bearer {accessToken}" \
  -H "Content-Type: application/json" \
  -d '{
    "description": "매월 책 2권 완독하는 독서 모임입니다 (수정)",
    "maxMembers": 15
  }'
```

#### Request Body
| 필드 | 타입 | 필수 | 제약조건 | 설명 |
|------|------|------|----------|------|
| name | String | N | 2-50자 | 챌린지 이름 |
| description | String | N | 최대 500자 | 챌린지 설명 |
| thumbnailImage | String | N | URL 형식 | 썸네일 이미지 |
| rules | String | N | 최대 1000자 | 챌린지 규칙 |
| maxMembers | Integer | N | 현재 인원 이상 | 최대 인원 (증가만 가능) |

> **주의**: supportAmount, depositAmount, supportDay는 활성화 후 변경 불가

### Response

#### Response 200 OK
```json
{
  "success": true,
  "data": {
    "challengeId": 1,
    "name": "책벌레들",
    "description": "매월 책 2권 완독하는 독서 모임입니다 (수정)",
    "maxMembers": 15,
    "updatedAt": "2026-01-14T10:30:00Z"
  },
  "message": "챌린지 정보가 수정되었습니다",
  "timestamp": "2026-01-14T10:30:00Z"
}
```

### Errors
| HTTP | 코드 | 메시지 |
|------|------|--------|
| 403 | CHALLENGE_004 | 리더만 수정할 수 있습니다 |
| 404 | CHALLENGE_001 | 챌린지를 찾을 수 없습니다 |

---

## 026. 챌린지 삭제 (모집 중 상태에서만)

### 기본 정보
- **Endpoint**: `DELETE /challenges/{challengeId}`
- **우선순위**: P1
- **인증**: 필요 (리더)

### Response

#### Response 200 OK
```json
{
  "success": true,
  "data": {
    "challengeId": 1,
    "deleted": true
  },
  "message": "챌린지가 삭제되었습니다",
  "timestamp": "2026-01-14T10:30:00Z"
}
```

### Errors
| HTTP | 코드 | 메시지 |
|------|------|--------|
| 400 | CHALLENGE_010 | 활성화된 챌린지는 삭제할 수 없습니다 |
| 403 | CHALLENGE_004 | 리더만 삭제할 수 있습니다 |
| 404 | CHALLENGE_001 | 챌린지를 찾을 수 없습니다 |

---

## 027. 내 챌린지 목록

### 기본 정보
- **Endpoint**: `GET /challenges/me`
- **우선순위**: P0
- **인증**: 필요

### Request

#### Request Syntax
```bash
curl -X GET "https://api.woorido.com/api/v1/challenges/me?role=FOLLOWER" \
  -H "Authorization: Bearer {accessToken}"
```

#### Query Parameters
| 파라미터 | 타입 | 필수 | 기본값 | 설명 |
|----------|------|------|--------|------|
| role | String | N | - | LEADER / FOLLOWER |
| status | String | N | - | ACTIVE / CLOSED |

### Response

#### Response 200 OK
```json
{
  "success": true,
  "data": {
    "challenges": [
      {
        "challengeId": 1,
        "name": "책벌레들",
        "status": "ACTIVE",
        "myRole": "FOLLOWER",
        "myStatus": "ACTIVE",
        "memberCount": {
          "current": 10,
          "max": 10
        },
        "supportAmount": 100000,
        "nextSupportDate": "2026-02-01",
        "thumbnailImage": "https://cdn.woorido.com/challenges/1.jpg",
        "upcomingMeeting": {
          "meetingId": 5,
          "title": "2월 정기모임",
          "scheduledAt": "2026-02-15T14:00:00Z"
        }
      }
    ],
    "summary": {
      "totalChallenges": 3,
      "asLeader": 1,
      "asFollower": 2,
      "monthlySupport": 300000
    }
  },
  "timestamp": "2026-01-14T10:30:00Z"
}
```

---

## 028. 챌린지 어카운트 조회

### 기본 정보
- **Endpoint**: `GET /challenges/{challengeId}/account`
- **우선순위**: P0
- **인증**: 필요 (멤버)

### Response

#### Response 200 OK
```json
{
  "success": true,
  "data": {
    "challengeId": 1,
    "balance": 5000000,
    "lockedDeposits": 2000000,
    "availableBalance": 5000000,
    "stats": {
      "totalSupport": 10000000,
      "totalExpense": 5000000,
      "totalFee": 150000,
      "monthlyAverage": {
        "support": 1000000,
        "expense": 500000
      }
    },
    "recentTransactions": [
      {
        "transactionId": 100,
        "type": "SUPPORT",
        "amount": 100000,
        "description": "홍길동 서포트 납입",
        "createdAt": "2026-01-01T10:00:00Z"
      }
    ],
    "supportStatus": {
      "thisMonth": {
        "paid": 9,
        "unpaid": 1,
        "total": 10
      }
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

## 029. 자동 납입 설정

### 기본 정보
- **Endpoint**: `PUT /challenges/{challengeId}/support/settings`
- **우선순위**: P1
- **인증**: 필요 (멤버)

### Request

#### Request Syntax
```bash
curl -X PUT https://api.woorido.com/api/v1/challenges/1/support/settings \
  -H "Authorization: Bearer {accessToken}" \
  -H "Content-Type: application/json" \
  -d '{
    "autoPayEnabled": true
  }'
```

#### Request Body
| 필드 | 타입 | 필수 | 설명 |
|------|------|------|------|
| autoPayEnabled | Boolean | Y | 자동 납입 활성화 여부 |

### Response

#### Response 200 OK
```json
{
  "success": true,
  "data": {
    "challengeId": 1,
    "autoPayEnabled": true,
    "nextPaymentDate": "2026-02-01",
    "amount": 100000
  },
  "message": "자동 납입이 설정되었습니다",
  "timestamp": "2026-01-14T10:30:00Z"
}
```

---

## 030. 챌린지 가입

### 기본 정보
- **Endpoint**: `POST /challenges/{challengeId}/join`
- **우선순위**: P0
- **인증**: 필요

### Response

#### Response 201 Created
```json
{
  "success": true,
  "data": {
    "memberId": 10,
    "challengeId": 1,
    "challengeName": "책벌레들",
    "role": "FOLLOWER",
    "status": "ACTIVE",
    "breakdown": {
      "entryFee": 125000,
      "deposit": 200000,
      "firstSupport": 100000,
      "total": 425000
    },
    "newBalance": 75000,
    "joinedAt": "2026-01-14T10:30:00Z"
  },
  "message": "챌린지에 가입되었습니다",
  "timestamp": "2026-01-14T10:30:00Z"
}
```

### 비즈니스 규칙
| 항목 | 설명 |
|------|------|
| 입회비 계산 | balance / (memberCount - 1) |
| 첫 서포트 | 납입일 7일 전 이내 가입 시 첫 달 서포트 포함 |
| 당도 제한 | 당도(Brix)가 음수인 사용자는 가입 불가 |
| 상태 전환 | 멤버 수 3명 도달 시 자동으로 ACTIVE(진행 중) 상태로 전환 (P-047) |

### Errors
| HTTP | 코드 | 메시지 |
|------|------|--------|
| 400 | ACCOUNT_004 | 잔액이 부족합니다 |
| 400 | CHALLENGE_002 | 이미 가입한 챌린지입니다 |
| 400 | CHALLENGE_005 | 챌린지 정원이 초과되었습니다 |
| 400 | CHALLENGE_006 | 모집 중인 챌린지가 아닙니다 |
| 400 | USER_004 | 당도가 음수인 사용자는 가입할 수 없습니다 |
| 404 | CHALLENGE_001 | 챌린지를 찾을 수 없습니다 |

---

## 031. 챌린지 탈퇴

### 기본 정보
- **Endpoint**: `DELETE /challenges/{challengeId}/leave`
- **우선순위**: P0
- **인증**: 필요

### Response

#### Response 200 OK
```json
{
  "success": true,
  "data": {
    "challengeId": 1,
    "challengeName": "책벌레들",
    "refund": {
      "deposit": 200000,
      "deducted": 0,
      "netRefund": 200000
    },
    "newBalance": 700000,
    "leftAt": "2026-01-14T10:30:00Z"
  },
  "message": "챌린지에서 탈퇴했습니다",
  "timestamp": "2026-01-14T10:30:00Z"
}
```

### Errors
| HTTP | 코드 | 메시지 |
|------|------|--------|
| 400 | MEMBER_002 | 리더는 탈퇴할 수 없습니다 (위임 후 탈퇴) |
| 403 | CHALLENGE_003 | 챌린지 멤버가 아닙니다 |
| 404 | CHALLENGE_001 | 챌린지를 찾을 수 없습니다 |

---

## 032. 멤버 목록 조회

### 기본 정보
- **Endpoint**: `GET /challenges/{challengeId}/members`
- **우선순위**: P0
- **인증**: 필요 (멤버)

### Response

#### Response 200 OK
```json
{
  "success": true,
  "data": {
    "members": [
      {
        "memberId": 1,
        "user": {
          "userId": 1,
          "nickname": "홍길동",
          "profileImage": "https://cdn.woorido.com/profiles/1.jpg",
          "brix": 85.5
        },
        "role": "LEADER",
        "status": "ACTIVE",
        "supportStatus": {
          "thisMonth": "PAID",
          "consecutivePaid": 12
        },
        "attendanceRate": 95.0,
        "joinedAt": "2025-12-01T10:00:00Z"
      }
    ],
    "summary": {
      "total": 10,
      "active": 8,
      "overdue": 1,
      "gracePeriod": 1
    }
  },
  "timestamp": "2026-01-14T10:30:00Z"
}
```

---

## 033. 멤버 상세 조회

### 기본 정보
- **Endpoint**: `GET /challenges/{challengeId}/members/{memberId}`
- **우선순위**: P1
- **인증**: 필요 (멤버)

### Response

#### Response 200 OK
```json
{
  "success": true,
  "data": {
    "memberId": 2,
    "user": {
      "userId": 2,
      "nickname": "김철수",
      "profileImage": "https://cdn.woorido.com/profiles/2.jpg",
      "brix": 72.0
    },
    "role": "FOLLOWER",
    "status": "ACTIVE",
    "stats": {
      "totalSupport": 1200000,
      "supportRate": 100.0,
      "attendanceRate": 85.0,
      "meetingsAttended": 10,
      "meetingsTotal": 12
    },
    "supportHistory": [
      {
        "month": "2026-01",
        "amount": 100000,
        "paidAt": "2026-01-01T10:00:00Z"
      }
    ],
    "joinedAt": "2025-12-15T10:00:00Z"
  },
  "timestamp": "2026-01-14T10:30:00Z"
}
```

### Errors
| HTTP | 코드 | 메시지 |
|------|------|--------|
| 403 | CHALLENGE_003 | 챌린지 멤버가 아닙니다 |
| 404 | MEMBER_001 | 멤버를 찾을 수 없습니다 |

---

## 034. 리더 위임

### 기본 정보
- **Endpoint**: `POST /challenges/{challengeId}/delegate`
- **우선순위**: P1
- **인증**: 필요 (리더)

### Request

#### Request Syntax
```bash
curl -X POST https://api.woorido.com/api/v1/challenges/1/delegate \
  -H "Authorization: Bearer {accessToken}" \
  -H "Content-Type: application/json" \
  -d '{
    "targetMemberId": 5
  }'
```

#### Request Body
| 필드 | 타입 | 필수 | 설명 |
|------|------|------|------|
| targetMemberId | Long | Y | 위임받을 멤버 ID |

### Response

#### Response 200 OK
```json
{
  "success": true,
  "data": {
    "challengeId": 1,
    "previousLeader": {
      "memberId": 1,
      "userId": 1,
      "nickname": "홍길동",
      "newRole": "FOLLOWER"
    },
    "newLeader": {
      "memberId": 5,
      "userId": 5,
      "nickname": "이영희",
      "newRole": "LEADER"
    },
    "delegatedAt": "2026-01-14T10:30:00Z"
  },
  "message": "리더 권한이 위임되었습니다",
  "timestamp": "2026-01-14T10:30:00Z"
}
```

### Errors
| HTTP | 코드 | 메시지 |
|------|------|--------|
| 400 | MEMBER_004 | 자신에게 위임할 수 없습니다 |
| 400 | MEMBER_005 | 정지된 멤버에게 위임할 수 없습니다 |
| 403 | CHALLENGE_004 | 리더만 위임할 수 있습니다 |
| 404 | MEMBER_001 | 멤버를 찾을 수 없습니다 |

---

**관련 문서:**
- [00_API_OVERVIEW.md](./00_API_OVERVIEW.md) - 공통 사항
- [03_API_ACCOUNT.md](./03_API_ACCOUNT.md) - 어카운트 API
- [05_API_MEETING.md](./05_API_MEETING.md) - 모임 API
