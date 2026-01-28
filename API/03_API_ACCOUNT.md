# ACCOUNT API

> **총 API**: 7개
> **담당**: Spring Boot
> **관련 테이블**: `accounts`, `account_transactions`

---

## 엔드포인트 목록

| # | Method | Endpoint | 설명 | 우선순위 | 인증 |
|---|--------|----------|------|---------|------|
| 015 | GET | /accounts/me | 내 어카운트 조회 | P0 | 필요 |
| 016 | GET | /accounts/me/transactions | 거래 내역 조회 | P0 | 필요 |
| 017 | POST | /accounts/charge | 크레딧 충전 | P0 | 필요 |
| 018 | POST | /accounts/charge/callback | 충전 콜백 (내부 API) | P0 | PG 서명 |
| 019 | POST | /accounts/withdraw | 출금 | P0 | 필요 |
| 020 | POST | /accounts/support | 서포트 수동 납입 | P0 | 필요 |
| 021 | GET | /accounts/fee-policy | 수수료 정책 조회 | P2 | 필요 |

---

## 015. 내 어카운트 조회

### 기본 정보
- **Endpoint**: `GET /accounts/me`
- **우선순위**: P0
- **인증**: 필요

### Request

#### Request Syntax
```bash
curl -X GET https://api.woorido.com/api/v1/accounts/me \
  -H "Authorization: Bearer {accessToken}"
```

### Response

#### Response 200 OK
```json
{
  "success": true,
  "data": {
    "accountId": 1,
    "userId": 1,
    "balance": 500000,
    "availableBalance": 450000,
    "lockedBalance": 50000,
    "limits": {
      "dailyWithdrawLimit": 1000000,
      "monthlyWithdrawLimit": 5000000,
      "usedToday": 100000,
      "usedThisMonth": 500000
    },
    "linkedBankAccount": {
      "bankCode": "088",
      "bankName": "신한은행",
      "accountNumber": "110-***-***789",
      "accountHolder": "홍길동",
      "isVerified": true
    },
    "createdAt": "2025-06-01T10:00:00Z"
  },
  "timestamp": "2026-01-14T10:30:00Z"
}
```

#### Response Elements
| 필드 | 타입 | 설명 |
|------|------|------|
| data.accountId | Long | 어카운트 ID |
| data.balance | Long | 총 잔액 |
| data.availableBalance | Long | 출금 가능 잔액 |
| data.lockedBalance | Long | 보증금 잠금 금액 |
| data.limits | Object | 출금 한도 정보 |
| data.linkedBankAccount | Object | 연결된 은행 계좌 정보 |

### Errors
| HTTP | 코드 | 메시지 |
|------|------|--------|
| 401 | AUTH_001 | 인증이 필요합니다 |

---

## 016. 거래 내역 조회

### 기본 정보
- **Endpoint**: `GET /accounts/me/transactions`
- **우선순위**: P0
- **인증**: 필요

### Request

#### Request Syntax
```bash
curl -X GET "https://api.woorido.com/api/v1/accounts/me/transactions?type=CHARGE&page=0&size=20" \
  -H "Authorization: Bearer {accessToken}"
```

#### Query Parameters
| 파라미터 | 타입 | 필수 | 기본값 | 설명 |
|----------|------|------|--------|------|
| type | String | N | - | TransactionType Enum |
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
        "transactionId": 1,
        "type": "CHARGE",
        "amount": 100000,
        "balance": 500000,
        "description": "크레딧 충전",
        "relatedChallenge": null,
        "createdAt": "2026-01-14T10:00:00Z"
      },
      {
        "transactionId": 2,
        "type": "SUPPORT",
        "amount": -100000,
        "balance": 400000,
        "description": "책벌레들 서포트 납입",
        "relatedChallenge": {
          "challengeId": 1,
          "name": "책벌레들"
        },
        "createdAt": "2026-01-14T11:00:00Z"
      }
    ],
    "page": {
      "number": 0,
      "size": 20,
      "totalElements": 2,
      "totalPages": 1
    },
    "summary": {
      "totalIncome": 100000,
      "totalExpense": 100000,
      "period": {
        "startDate": "2026-01-01",
        "endDate": "2026-01-14"
      }
    }
  },
  "timestamp": "2026-01-14T10:30:00Z"
}
```

---

## 017. 크레딧 충전

### 기본 정보
- **Endpoint**: `POST /accounts/charge`
- **우선순위**: P0
- **인증**: 필요

### Request

#### Request Syntax
```bash
curl -X POST https://api.woorido.com/api/v1/accounts/charge \
  -H "Authorization: Bearer {accessToken}" \
  -H "Content-Type: application/json" \
  -d '{
    "amount": 100000,
    "paymentMethod": "CARD",
    "returnUrl": "https://woorido.com/payment/complete"
  }'
```

#### Request Body
| 필드 | 타입 | 필수 | 제약조건 | 설명 |
|------|------|------|----------|------|
| amount | Long | Y | 10,000원 이상, 10,000원 단위 | 충전 금액 |
| paymentMethod | String | Y | CARD / BANK_TRANSFER | 결제 수단 |
| returnUrl | String | Y | URL 형식 | 결제 완료 후 리턴 URL |

### Response

#### Response 200 OK
```json
{
  "success": true,
  "data": {
    "orderId": "ORD20260114103000001",
    "amount": 100000,
    "fee": 3000,
    "totalPaymentAmount": 103000,
    "paymentUrl": "https://pay.woorido.com/checkout/ORD20260114103000001",
    "expiresAt": "2026-01-14T10:45:00Z"
  },
  "message": "수수료 포함 103,000원 결제 페이지로 이동합니다",
  "timestamp": "2026-01-14T10:30:00Z"
}
```

### 수수료 정책 (P-011)
- 10,000 ~ 200,000원: 3% 부과
- 예: 100,000원 충전 시 103,000원 결제
```

### Errors
| HTTP | 코드 | 메시지 |
|------|------|--------|
| 400 | ACCOUNT_002 | 충전 금액은 10,000원 이상이어야 합니다 |
| 400 | ACCOUNT_007 | 충전 금액은 10,000원 단위여야 합니다 |
| 400 | ACCOUNT_008 | 결제 수단은 CARD 또는 BANK_TRANSFER 만 가능합니다 |

---

## 018. 충전 콜백 (내부 API)

### 기본 정보
- **Endpoint**: `POST /accounts/charge/callback`
- **우선순위**: P0
- **인증**: 불필요 (PG 서명 검증)

### Request

#### Request Syntax
```bash
curl -X POST https://api.woorido.com/api/v1/accounts/charge/callback \
  -H "Content-Type: application/json" \
  -d '{
    "orderId": "ORD20260114103000001",
    "paymentKey": "pg_payment_key_xxx",
    "amount": 100000,
    "status": "SUCCESS"
  }'
```

#### Request Body
| 필드 | 타입 | 필수 | 설명 |
|------|------|------|------|
| orderId | String | Y | 주문 ID |
| paymentKey | String | Y | PG 결제 키 |
| amount | Long | Y | 결제 금액 |
| status | String | Y | SUCCESS / FAILED |

### Response

#### Response 200 OK
```json
{
  "success": true,
  "data": {
    "transactionId": 1,
    "orderId": "ORD20260114103000001",
    "amount": 100000,
    "newBalance": 600000,
    "completedAt": "2026-01-14T10:35:00Z"
  },
  "timestamp": "2026-01-14T10:35:00Z"
}
```

---

## 019. 출금

### 기본 정보
- **Endpoint**: `POST /accounts/withdraw`
- **우선순위**: P0
- **인증**: 필요

### Request

#### Request Syntax
```bash
curl -X POST https://api.woorido.com/api/v1/accounts/withdraw \
  -H "Authorization: Bearer {accessToken}" \
  -H "Content-Type: application/json" \
  -d '{
    "amount": 100000,
    "bankCode": "088",
    "accountNumber": "110-123-456789",
    "accountHolder": "홍길동"
  }'
```

#### Request Body
| 필드 | 타입 | 필수 | 제약조건 | 설명 |
|------|------|------|----------|------|
| amount | Long | Y | 1원 이상, 가용액 이하 | 출금 금액 |
| bankCode | String | Y | 3자리 | 은행 코드 |
| accountNumber | String | Y | - | 계좌 번호 |
| accountHolder | String | Y | - | 예금주명 |

### Response

#### Response 200 OK
```json
{
  "success": true,
  "data": {
    "withdrawId": 1,
    "amount": 100000,
    "fee": 0,
    "netAmount": 100000,
    "newBalance": 400000,
    "bankInfo": {
      "bankCode": "088",
      "bankName": "신한은행",
      "accountNumber": "110-***-***789"
    },
    "estimatedArrival": "2026-01-14T12:00:00Z",
    "createdAt": "2026-01-14T10:30:00Z"
  },
  "message": "출금이 요청되었습니다",
  "timestamp": "2026-01-14T10:30:00Z"
}
```

### 수수료 정보
- 출금 수수료 무료 (충전 시 부과됨)

### Errors
| HTTP | 코드 | 메시지 |
|------|------|--------|
| 400 | ACCOUNT_003 | 출금 가능 금액을 초과했습니다 |
| 400 | ACCOUNT_004 | 잔액이 부족합니다 |
| 400 | ACCOUNT_005 | 일일 출금 한도를 초과했습니다 |
| 400 | ACCOUNT_006 | 월간 출금 한도를 초과했습니다 |

---

## 020. 서포트 수동 납입

### 기본 정보
- **Endpoint**: `POST /accounts/support`
- **우선순위**: P0
- **인증**: 필요

### Request

#### Request Syntax
```bash
curl -X POST https://api.woorido.com/api/v1/accounts/support \
  -H "Authorization: Bearer {accessToken}" \
  -H "Content-Type: application/json" \
  -d '{
    "challengeId": 1
  }'
```

#### Request Body
| 필드 | 타입 | 필수 | 설명 |
|------|------|------|------|
| challengeId | Long | Y | 챌린지 ID |

### Response

#### Response 200 OK
```json
{
  "success": true,
  "data": {
    "transactionId": 1,
    "challengeId": 1,
    "challengeName": "책벌레들",
    "amount": 100000,
    "newBalance": 400000,
    "newChallengeBalance": 1100000,
    "isFirstSupport": false,
    "createdAt": "2026-01-14T10:30:00Z"
  },
  "message": "서포트가 납입되었습니다",
  "timestamp": "2026-01-14T10:30:00Z"
}
```

### Errors
| HTTP | 코드 | 메시지 |
|------|------|--------|
| 400 | ACCOUNT_004 | 잔액이 부족합니다 |
| 400 | SUPPORT_001 | 이미 이번 달 서포트를 납입했습니다 |
| 403 | CHALLENGE_003 | 챌린지 멤버가 아닙니다 |
| 404 | CHALLENGE_001 | 챌린지를 찾을 수 없습니다 |

---

## 021. 수수료 정책 조회

### 기본 정보
- **Endpoint**: `GET /accounts/fee-policy`
- **우선순위**: P2
- **인증**: 필요

### Request

#### Request Syntax
```bash
curl -X GET https://api.woorido.com/api/v1/accounts/fee-policy \
  -H "Authorization: Bearer {accessToken}"
```

### Response

#### Response 200 OK
```json
{
  "success": true,
  "data": {
    "withdrawFee": {
      "rate": 0,
      "minAmount": 0,
      "maxAmount": null
    },
    "chargeFee": {
      "tiers": [
        {
          "name": "소액",
          "minAmount": 0,
          "maxAmount": 9999,
          "rate": 1.0
        },
        {
          "name": "일반",
          "minAmount": 10000,
          "maxAmount": 200000,
          "rate": 3.0
        },
        {
          "name": "고액",
          "minAmount": 200001,
          "maxAmount": null,
          "rate": 1.5
        }
      ]
    },
    "updatedAt": "2026-01-01T00:00:00Z"
  },
  "timestamp": "2026-01-14T10:30:00Z"
}
```

---

**관련 문서:**
- [00_API_OVERVIEW.md](./00_API_OVERVIEW.md) - 공통 사항
- [02_API_USER.md](./02_API_USER.md) - 사용자 API
- [04_API_CHALLENGE.md](./04_API_CHALLENGE.md) - 챌린지 API
