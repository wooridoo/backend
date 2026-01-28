# SYSTEM API

> **총 API**: 12개 (신고 2개 + 알림 5개 + 환불 2개 + 정산 2개 + 지출 1개)
> **담당**: Spring Boot
> **관련 테이블**: `reports`, `notifications`, `refunds`, `settlements`

---

## 엔드포인트 목록

### 신고 (REPORT)

| # | Method | Endpoint | 설명 | 우선순위 | 인증 |
|---|--------|----------|------|---------|------|
| 072 | POST | /reports | 신고하기 | P1 | 필요 |
| 073 | GET | /reports/my | 내 신고 목록 조회 | P2 | 필요 |

### 알림 (NOTIFICATION)

| # | Method | Endpoint | 설명 | 우선순위 | 인증 |
|---|--------|----------|------|---------|------|
| 074 | GET | /notifications | 알림 목록 조회 | P0 | 필요 |
| 075 | GET | /notifications/{notificationId} | 알림 상세 조회 | P1 | 필요 |
| 076 | PUT | /notifications/{notificationId}/read | 알림 읽음 처리 | P0 | 필요 |
| 077 | PUT | /notifications/read-all | 전체 알림 읽음 처리 | P1 | 필요 |
| 078 | GET/PUT | /notifications/settings | 알림 설정 조회/수정 | P2 | 필요 |

### 환불 (REFUND)

| # | Method | Endpoint | 설명 | 우선순위 | 인증 |
|---|--------|----------|------|---------|------|
| 079 | POST | /refunds | 환불 요청 | P1 | 필요 |
| 080 | GET | /refunds/{refundId} | 환불 상태 조회 | P2 | 필요 |

### 정산 (SETTLEMENT)

| # | Method | Endpoint | 설명 | 우선순위 | 인증 |
|---|--------|----------|------|---------|------|
| 081 | GET | /challenges/{challengeId}/settlement | 정산 내역 조회 | P1 | 멤버 |
| 082 | POST | /challenges/{challengeId}/settlement/process | 정산 실행 | P1 | 관리자 |

---

## 072. 신고하기

### 기본 정보
- **Endpoint**: `POST /reports`
- **우선순위**: P1
- **인증**: 필요

### Request

#### Request Syntax
```bash
curl -X POST https://api.woorido.com/api/v1/reports \
  -H "Authorization: Bearer {accessToken}" \
  -H "Content-Type: application/json" \
  -d '{
    "targetType": "COMMENT",
    "targetId": 15,
    "reason": "ABUSE",
    "description": "욕설과 비방이 포함된 댓글입니다. 모임 분위기를 해치고 있습니다.",
    "evidenceUrls": [
      "https://cdn.woorido.com/evidence/screenshot1.jpg"
    ]
  }'
```

#### Request Body
| 필드 | 타입 | 필수 | 제약조건 | 설명 |
|------|------|------|----------|------|
| targetType | String | Y | USER/POST/COMMENT/CHALLENGE | 신고 대상 유형 |
| targetId | Long | Y | - | 신고 대상 ID |
| reason | String | Y | ReportReason Enum | 신고 사유 |
| description | String | Y | 10~1000자 | 상세 설명 |
| evidenceUrls | Array | N | 최대 5개 | 증거 이미지 URL |

#### ReportReason Enum
| 값 | 설명 |
|----|------|
| SPAM | 스팸/광고 |
| ABUSE | 욕설/비방 |
| FRAUD | 사기/허위 정보 |
| INAPPROPRIATE | 부적절한 콘텐츠 |
| OTHER | 기타 |

### Response

#### Response 201 Created
```json
{
  "success": true,
  "data": {
    "reportId": 5,
    "targetType": "COMMENT",
    "targetId": 15,
    "status": "PENDING",
    "createdAt": "2026-01-14T10:30:00Z"
  },
  "message": "신고가 접수되었습니다",
  "timestamp": "2026-01-14T10:30:00Z"
}
```

```

### 비즈니스 규칙
- **중복 신고 제한**: 동일 대상에 대해 1회만 신고 가능
- **제재 정책**: 누적 신고 20회 도달 시 자동 이용 정지 (P-031)

### Errors
| HTTP | 코드 | 메시지 |
|------|------|--------|
| 400 | VALIDATION_001 | 상세 설명은 10자 이상 입력해주세요 |
| 404 | REPORT_002 | 신고 대상을 찾을 수 없습니다 |
| 409 | REPORT_001 | 이미 신고한 대상입니다 |

---

## 071. 알림 목록 조회

### 기본 정보
- **Endpoint**: `GET /notifications`
- **우선순위**: P0
- **인증**: 필요

### Request

#### Request Syntax
```bash
curl -X GET "https://api.woorido.com/api/v1/notifications?type=PAYMENT&isRead=false&page=0" \
  -H "Authorization: Bearer {accessToken}"
```

#### Query Parameters
| 파라미터 | 타입 | 필수 | 기본값 | 설명 |
|----------|------|------|--------|------|
| page | Integer | N | 0 | 페이지 번호 |
| size | Integer | N | 20 | 페이지 크기 |
| type | String | N | - | ALL/CHALLENGE/PAYMENT/SOCIAL/SYSTEM |
| isRead | Boolean | N | - | 읽음 여부 필터 |

### Response

#### Response 200 OK
```json
{
  "success": true,
  "data": {
    "content": [
      {
        "notificationId": 101,
        "type": "PAYMENT",
        "title": "납입일 알림",
        "message": "내일은 '2026 저축 챌린지'의 납입일입니다. 잔액을 확인해주세요.",
        "isRead": false,
        "data": {
          "challengeId": 1,
          "actionUrl": "/challenges/1"
        },
        "createdAt": "2026-01-14T09:00:00Z"
      }
    ],
    "totalElements": 45,
    "totalPages": 3,
    "number": 0,
    "size": 20,
    "unreadCount": 12
  },
  "timestamp": "2026-01-14T10:30:00Z"
}
```

---

## 073. 알림 읽음 처리

### 기본 정보
- **Endpoint**: `PUT /notifications/{notificationId}/read`
- **우선순위**: P0
- **인증**: 필요

### Response

#### Response 200 OK
```json
{
  "success": true,
  "data": {
    "notificationId": 101,
    "isRead": true,
    "readAt": "2026-01-14T10:30:00Z"
  },
  "message": "알림을 읽음 처리했습니다",
  "timestamp": "2026-01-14T10:30:00Z"
}
```

### Errors
| HTTP | 코드 | 메시지 |
|------|------|--------|
| 404 | NOTIFICATION_001 | 알림을 찾을 수 없습니다 |

---

## 074. 전체 알림 읽음 처리

### 기본 정보
- **Endpoint**: `PUT /notifications/read-all`
- **우선순위**: P1
- **인증**: 필요

### Response

#### Response 200 OK
```json
{
  "success": true,
  "data": {
    "readCount": 12
  },
  "message": "12개의 알림을 읽음 처리했습니다",
  "timestamp": "2026-01-14T10:30:00Z"
}
```

---

## 075. 알림 설정 조회/수정

### 기본 정보
- **Endpoint**: `GET/PUT /notifications/settings`
- **우선순위**: P2
- **인증**: 필요

### Request (PUT)

#### Request Body
| 필드 | 타입 | 필수 | 설명 |
|------|------|------|------|
| pushEnabled | Boolean | N | 푸시 알림 활성화 |
| emailEnabled | Boolean | N | 이메일 알림 활성화 |
| challengeAlerts | Boolean | N | 챌린지 관련 알림 |
| paymentAlerts | Boolean | N | 결제 관련 알림 |
| socialAlerts | Boolean | N | 소셜 활동 알림 |
| marketingAlerts | Boolean | N | 마케팅 알림 |

### Response

#### Response 200 OK
```json
{
  "success": true,
  "data": {
    "pushEnabled": true,
    "emailEnabled": false,
    "challengeAlerts": true,
    "paymentAlerts": true,
    "socialAlerts": true,
    "marketingAlerts": false
  },
  "message": "알림 설정이 변경되었습니다",
  "timestamp": "2026-01-14T10:30:00Z"
}
```

---

## 076. 환불 요청

### 기본 정보
- **Endpoint**: `POST /refunds`
- **우선순위**: P1
- **인증**: 필요

### Request

#### Request Syntax
```bash
curl -X POST https://api.woorido.com/api/v1/refunds \
  -H "Authorization: Bearer {accessToken}" \
  -H "Content-Type: application/json" \
  -d '{
    "challengeId": 1,
    "reason": "개인 사정으로 인해 챌린지에 더 이상 참여하기 어렵습니다."
  }'
```

#### Request Body
| 필드 | 타입 | 필수 | 제약조건 | 설명 |
|------|------|------|----------|------|
| challengeId | Long | Y | - | 챌린지 ID |
| reason | String | Y | 10~500자 | 환불 사유 |
| amount | Long | N | - | 부분 환불 금액 |

### Response

#### Response 200 OK
```json
{
  "success": true,
  "data": {
    "refundId": 10,
    "challengeId": 1,
    "requestedAmount": 50000,
    "estimatedAmount": 50000,
    "fee": 0,
    "status": "PENDING",
    "createdAt": "2026-01-14T10:30:00Z"
  },
  "message": "환불 요청이 접수되었습니다",
  "timestamp": "2026-01-14T10:30:00Z"
}
```

### 비즈니스 규칙
| 상황 | 환불 정책 |
|------|----------|
| 모집 중 탈퇴 | 입회비 전액 환불 |
| 진행 중 | 환불 불가 (탈퇴 불가 정책) |
| 완료 후 | 정산 후 자동 환급 |

### Errors
| HTTP | 코드 | 메시지 |
|------|------|--------|
| 400 | REFUND_001 | 현재 상태에서는 환불이 불가합니다 |
| 400 | REFUND_002 | 환불 금액이 납입 금액을 초과합니다 |
| 404 | CHALLENGE_001 | 챌린지를 찾을 수 없습니다 |

---

## 078. 정산 내역 조회

### 기본 정보
- **Endpoint**: `GET /challenges/{challengeId}/settlement`
- **우선순위**: P1
- **인증**: 필요 (멤버)

### Response

#### Response 200 OK
```json
{
  "success": true,
  "data": {
    "settlementId": 1,
    "challengeId": 1,
    "challengeTitle": "2026 저축 챌린지",
    "status": "COMPLETED",
    "totalPool": 6000000,
    "totalExpense": 320000,
    "netPool": 5680000,
    "memberCount": 10,
    "memberSettlements": [
      {
        "userId": 1,
        "nickname": "홍길동",
        "totalDeposit": 600000,
        "shareRatio": 10.56,
        "settlementAmount": 600000,
        "penaltyAmount": 0,
        "netAmount": 600000,
        "status": "COMPLETED"
      }
    ],
    "processedAt": "2026-12-31T00:00:00Z",
    "completedAt": "2026-12-31T12:00:00Z"
  },
  "timestamp": "2026-01-14T10:30:00Z"
}
```

### Errors
| HTTP | 코드 | 메시지 |
|------|------|--------|
| 403 | MEMBER_001 | 챌린지에 참여하지 않았습니다 |
| 404 | CHALLENGE_001 | 챌린지를 찾을 수 없습니다 |
| 404 | SETTLEMENT_001 | 정산 정보가 없습니다 |

---

## 079. 정산 실행 (시스템/관리자)

### 기본 정보
- **Endpoint**: `POST /challenges/{challengeId}/settlement/process`
- **우선순위**: P1
- **인증**: 필요 (시스템/관리자)

### Response

#### Response 200 OK
```json
{
  "success": true,
  "data": {
    "settlementId": 1,
    "challengeId": 1,
    "status": "IN_PROGRESS",
    "totalPool": 6000000,
    "memberCount": 10,
    "estimatedCompletion": "2026-01-14T12:00:00Z"
  },
  "message": "정산 처리가 시작되었습니다",
  "timestamp": "2026-01-14T10:30:00Z"
}
```

### 정산 규칙
| 단계 | 설명 |
|------|------|
| 1. 지출 차감 | 총 적립금에서 승인된 지출 금액 차감 (순 적립금 산정) |
| 2. 기본 배분 | 순 적립금을 개인별 납입 비율에 따라 배분 |
| 3. 패널티 차감 | 미납/결석 횟수에 따른 패널티 차감 |
| 4. 수수료 적용 | P-021 정책에 따른 수수료 차감 |
| 5. 최종 입금 | 정산액 계좌 입금 |

> **주의**: 60일 미충전 자동 탈퇴자의 보증금은 **전액 몰수**되어 시스템에 귀속됩니다 (P-052).

### Errors
| HTTP | 코드 | 메시지 |
|------|------|--------|
| 400 | CHALLENGE_010 | 진행 중인 챌린지는 정산할 수 없습니다 |
| 400 | SETTLEMENT_002 | 이미 정산이 완료되었습니다 |
| 403 | AUTH_015 | 관리자 권한이 필요합니다 |
| 404 | CHALLENGE_001 | 챌린지를 찾을 수 없습니다 |

---

**관련 문서:**
- [00_API_OVERVIEW.md](./00_API_OVERVIEW.md) - 공통 사항
- [07_API_SNS.md](./07_API_SNS.md) - SNS API
- [10_API_DJANGO.md](./10_API_DJANGO.md) - Django API (검색/분석/추천)
