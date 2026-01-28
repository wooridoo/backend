# LEDGER API (장부)

> **총 API**: 5개 (기존 2개 + 신규 3개)
> **담당**: Spring Boot
> **관련 테이블**: `ledger_entries`, `expenses`, `transactions`

---

## 엔드포인트 목록

| # | Method | Endpoint | 설명 | 우선순위 | 인증 |
|---|--------|----------|------|---------|------|
| 052 | GET | /challenges/{challengeId}/ledger | 장부 조회 | P1 | 멤버 |
| 053 | GET | /challenges/{challengeId}/ledger/export | 장부 내보내기 | P2 | 멤버 |
| **054** | **GET** | **/challenges/{challengeId}/ledger/summary** | **장부 요약** | **P1** | **멤버** |
| **055** | **POST** | **/challenges/{challengeId}/ledger** | **장부 등록** | **P1** | **리더** |
| **056** | **PUT** | **/ledger/{entryId}** | **장부 수정** | **P1** | **리더** |

---

## 052. 장부 조회

### 기본 정보
- **Endpoint**: `GET /challenges/{challengeId}/ledger`
- **우선순위**: P1
- **인증**: 필요 (멤버)

### Request

#### Request Syntax
```bash
curl -X GET "https://api.woorido.com/api/v1/challenges/1/ledger?page=0&size=20" \
  -H "Authorization: Bearer {accessToken}"
```

#### Query Parameters
| 파라미터 | 타입 | 필수 | 기본값 | 설명 |
|----------|------|------|--------|------|
| type | String | N | - | INCOME / EXPENSE |
| startDate | String | N | - | 조회 시작일 (YYYY-MM-DD) |
| endDate | String | N | - | 조회 종료일 (YYYY-MM-DD) |
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
        "entryId": 1,
        "type": "INCOME",
        "category": "SUPPORT",
        "amount": 100000,
        "description": "홍길동 서포트 납입",
        "relatedUser": {
          "userId": 1,
          "nickname": "홍길동"
        },
        "createdAt": "2026-01-01T10:00:00Z"
      },
      {
        "entryId": 2,
        "type": "EXPENSE",
        "category": "MEETING",
        "amount": -50000,
        "description": "1월 모임 장소 대여비",
        "approvedBy": {
          "userId": 1,
          "nickname": "홍길동"
        },
        "createdAt": "2026-01-15T14:00:00Z"
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

### Errors
| HTTP | 코드 | 메시지 |
|------|------|--------|
| 403 | CHALLENGE_003 | 챌린지 멤버가 아닙니다 |
| 404 | CHALLENGE_001 | 챌린지를 찾을 수 없습니다 |

---

## 053. 장부 내보내기

### 기본 정보
- **Endpoint**: `GET /challenges/{challengeId}/ledger/export`
- **우선순위**: P2
- **인증**: 필요 (멤버)

### Request

#### Query Parameters
| 파라미터 | 타입 | 필수 | 기본값 | 설명 |
|----------|------|------|--------|------|
| format | String | N | CSV | CSV / EXCEL |
| startDate | String | N | - | 시작일 |
| endDate | String | N | - | 종료일 |

### Response

#### Response 200 OK
```json
{
  "success": true,
  "data": {
    "downloadUrl": "https://cdn.woorido.com/exports/ledger_challenge_1_20260114.csv",
    "fileName": "장부_책벌레들_20260114.csv",
    "expiresAt": "2026-01-14T11:30:00Z"
  },
  "message": "다운로드 링크가 생성되었습니다",
  "timestamp": "2026-01-14T10:30:00Z"
}
```

---

## 054. 장부 요약 ⭐ NEW

### 기본 정보
- **Endpoint**: `GET /challenges/{challengeId}/ledger/summary`
- **우선순위**: P1
- **인증**: 필요 (멤버)

### Request

#### Request Syntax
```bash
curl -X GET "https://api.woorido.com/api/v1/challenges/1/ledger/summary?period=MONTH" \
  -H "Authorization: Bearer {accessToken}"
```

#### Query Parameters
| 파라미터 | 타입 | 필수 | 기본값 | 설명 |
|----------|------|------|--------|------|
| period | String | N | MONTH | WEEK / MONTH / YEAR / ALL |

### Response

#### Response 200 OK
```json
{
  "success": true,
  "data": {
    "challengeId": 1,
    "period": "MONTH",
    "periodRange": {
      "start": "2026-01-01",
      "end": "2026-01-31"
    },
    "balance": {
      "current": 850000,
      "lockedDeposit": 200000,
      "available": 650000
    },
    "summary": {
      "totalIncome": 1000000,
      "totalExpense": 150000,
      "netAmount": 850000
    },
    "breakdown": {
      "income": {
        "support": 900000,
        "entryFee": 100000,
        "other": 0
      },
      "expense": {
        "meeting": 80000,
        "food": 50000,
        "supplies": 20000,
        "other": 0
      }
    },
    "memberStats": {
      "paidCount": 9,
      "unpaidCount": 1,
      "totalMembers": 10,
      "paymentRate": 90.0
    },
    "trends": {
      "incomeChange": 5.5,
      "expenseChange": -2.3
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

## 055. 장부 등록 ⭐ NEW

### 기본 정보
- **Endpoint**: `POST /challenges/{challengeId}/ledger`
- **우선순위**: P1
- **인증**: 필요 (리더)

### Request

#### Request Syntax
```bash
curl -X POST https://api.woorido.com/api/v1/challenges/1/ledger \
  -H "Authorization: Bearer {accessToken}" \
  -H "Content-Type: application/json" \
  -d '{
    "type": "EXPENSE",
    "category": "MEETING",
    "amount": 50000,
    "description": "2월 모임 장소 대여비",
    "transactionDate": "2026-02-15"
  }'
```

#### Request Body
| 필드 | 타입 | 필수 | 제약조건 | 설명 |
|------|------|------|----------|------|
| type | String | Y | INCOME / EXPENSE | 장부 유형 |
| category | String | Y | LedgerCategory Enum | 카테고리 |
| amount | Long | Y | 1원 이상 | 금액 |
| description | String | Y | 최대 200자 | 설명 |
| transactionDate | String | N | YYYY-MM-DD | 거래일 (미입력 시 오늘) |

#### LedgerCategory Enum
| 타입 | 카테고리 | 설명 |
|------|----------|------|
| INCOME | SUPPORT | 서포트 납입 |
| INCOME | ENTRY_FEE | 입회비 |
| INCOME | OTHER_INCOME | 기타 수입 |
| EXPENSE | MEETING | 모임비 |
| EXPENSE | FOOD | 식비/다과 |
| EXPENSE | SUPPLIES | 물품비 |
| EXPENSE | OTHER_EXPENSE | 기타 지출 |

### Response

#### Response 201 Created
```json
{
  "success": true,
  "data": {
    "entryId": 51,
    "type": "EXPENSE",
    "category": "MEETING",
    "amount": 50000,
    "description": "2월 모임 장소 대여비",
    "newBalance": 800000,
    "createdBy": {
      "userId": 1,
      "nickname": "홍길동"
    },
    "createdAt": "2026-01-14T10:30:00Z"
  },
  "message": "장부가 등록되었습니다",
  "timestamp": "2026-01-14T10:30:00Z"
}
```

### Errors
| HTTP | 코드 | 메시지 |
|------|------|--------|
| 400 | LEDGER_001 | 금액은 1원 이상이어야 합니다 |
| 400 | LEDGER_002 | 챌린지 잔액이 부족합니다 (지출 시) |
| 403 | CHALLENGE_004 | 리더만 장부를 등록할 수 있습니다 |
| 404 | CHALLENGE_001 | 챌린지를 찾을 수 없습니다 |

---

## 056. 장부 수정 ⭐ NEW

### 기본 정보
- **Endpoint**: `PUT /ledger/{entryId}`
- **우선순위**: P1
- **인증**: 필요 (리더)

### Request

#### Request Syntax
```bash
curl -X PUT https://api.woorido.com/api/v1/ledger/51 \
  -H "Authorization: Bearer {accessToken}" \
  -H "Content-Type: application/json" \
  -d '{
    "category": "FOOD",
    "amount": 45000,
    "description": "2월 모임 다과비 (수정)"
  }'
```

#### Path Parameters
| 파라미터 | 타입 | 필수 | 설명 |
|----------|------|------|------|
| entryId | Long | Y | 장부 항목 ID |

#### Request Body
| 필드 | 타입 | 필수 | 제약조건 | 설명 |
|------|------|------|----------|------|
| category | String | N | LedgerCategory Enum | 카테고리 |
| amount | Long | N | 1원 이상 | 금액 |
| description | String | N | 최대 200자 | 설명 |
| transactionDate | String | N | YYYY-MM-DD | 거래일 |

### Response

#### Response 200 OK
```json
{
  "success": true,
  "data": {
    "entryId": 51,
    "type": "EXPENSE",
    "category": "FOOD",
    "amount": 45000,
    "description": "2월 모임 다과비 (수정)",
    "previousAmount": 50000,
    "amountDiff": -5000,
    "newBalance": 805000,
    "updatedBy": {
      "userId": 1,
      "nickname": "홍길동"
    },
    "updatedAt": "2026-01-14T11:00:00Z"
  },
  "message": "장부가 수정되었습니다",
  "timestamp": "2026-01-14T11:00:00Z"
}
```

### 비즈니스 규칙
- 금액 수정 시 잔액 자동 재계산
- 수정 이력 자동 기록

### Errors
| HTTP | 코드 | 메시지 |
|------|------|--------|
| 400 | LEDGER_001 | 금액은 1원 이상이어야 합니다 |
| 400 | LEDGER_002 | 챌린지 잔액이 부족합니다 |
| 400 | LEDGER_003 | 정산 완료된 항목은 수정할 수 없습니다 |
| 403 | CHALLENGE_004 | 리더만 장부를 수정할 수 있습니다 |
| 404 | LEDGER_004 | 장부 항목을 찾을 수 없습니다 |

---

**관련 문서:**
- [00_API_OVERVIEW.md](./00_API_OVERVIEW.md) - 공통 사항
- [04_API_CHALLENGE.md](./04_API_CHALLENGE.md) - 챌린지 API
- [08_API_SYSTEM.md](./08_API_SYSTEM.md) - 시스템 API (정산)
