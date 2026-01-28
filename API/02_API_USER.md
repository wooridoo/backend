# USER API

> **총 API**: 6개
> **담당**: Spring Boot
> **관련 테이블**: `users`, `accounts`, `challenge_members`

---

## 엔드포인트 목록

| # | Method | Endpoint | 설명 | 우선순위 | 인증 |
|---|--------|----------|------|---------|------|
| 009 | GET | /users/me | 내 정보 조회 | P0 | 필요 |
| 010 | PUT | /users/me | 내 정보 수정 | P0 | 필요 |
| 011 | PUT | /users/me/password | 비밀번호 변경 | P1 | 필요 |
| 012 | DELETE | /users/me | 회원 탈퇴 | P1 | 필요 |
| 013 | GET | /users/{userId} | 사용자 조회 | P1 | 필요 |
| 014 | GET | /users/check-nickname | 닉네임 중복 체크 | P0 | 불필요 |

---

## 009. 내 정보 조회

### 기본 정보
- **Endpoint**: `GET /users/me`
- **우선순위**: P0
- **인증**: 필요

### Request

#### Request Syntax
```bash
curl -X GET https://api.woorido.com/api/v1/users/me \
  -H "Authorization: Bearer {accessToken}"
```

### Response

#### Response 200 OK
```json
{
  "success": true,
  "data": {
    "userId": 1,
    "email": "user@example.com",
    "nickname": "홍길동",
    "phone": "010-1234-5678",
    "birthDate": "1990-01-15",
    "profileImage": "https://cdn.woorido.com/profiles/1.jpg",
    "status": "ACTIVE",
    "brix": 85.5,
    "account": {
      "accountId": 1,
      "balance": 500000,
      "availableBalance": 450000,
      "lockedBalance": 50000
    },
    "stats": {
      "challengeCount": 3,
      "completedChallenges": 2,
      "totalSupportAmount": 1500000
    },
    "createdAt": "2025-06-01T10:00:00Z",
    "updatedAt": "2026-01-14T10:30:00Z"
  },
  "timestamp": "2026-01-14T10:30:00Z"
}
```

#### Response Elements
| 필드 | 타입 | 설명 |
|------|------|------|
| data.userId | Long | 사용자 ID |
| data.email | String | 이메일 |
| data.nickname | String | 닉네임 |
| data.phone | String | 휴대폰 번호 |
| data.birthDate | String | 생년월일 |
| data.profileImage | String | 프로필 이미지 URL |
| data.status | String | 상태 (UserStatus) |
| data.brix | Double | 당도 점수 (0-100) |
| data.account | Object | 어카운트 정보 |
| data.stats | Object | 통계 정보 |

### Errors
| HTTP | 코드 | 메시지 |
|------|------|--------|
| 401 | AUTH_001 | 인증이 필요합니다 |

---

## 010. 내 정보 수정

### 기본 정보
- **Endpoint**: `PUT /users/me`
- **우선순위**: P0
- **인증**: 필요

### Request

#### Request Syntax
```bash
curl -X PUT https://api.woorido.com/api/v1/users/me \
  -H "Authorization: Bearer {accessToken}" \
  -H "Content-Type: application/json" \
  -d '{
    "nickname": "새닉네임",
    "profileImage": "https://cdn.woorido.com/profiles/new.jpg"
  }'
```

#### Request Body
| 필드 | 타입 | 필수 | 제약조건 | 설명 |
|------|------|------|----------|------|
| nickname | String | N | 2-20자 | 닉네임 |
| phone | String | N | 010-XXXX-XXXX | 휴대폰 번호 |
| profileImage | String | N | URL 형식 | 프로필 이미지 URL |

### Response

#### Response 200 OK
```json
{
  "success": true,
  "data": {
    "userId": 1,
    "nickname": "새닉네임",
    "phone": "010-1234-5678",
    "profileImage": "https://cdn.woorido.com/profiles/new.jpg",
    "updatedAt": "2026-01-14T10:30:00Z"
  },
  "message": "정보가 수정되었습니다",
  "timestamp": "2026-01-14T10:30:00Z"
}
```

### Errors
| HTTP | 코드 | 메시지 |
|------|------|--------|
| 400 | USER_006 | 닉네임은 2-20자여야 합니다 |
| 409 | USER_007 | 이미 사용 중인 닉네임입니다 |

---

## 011. 비밀번호 변경

### 기본 정보
- **Endpoint**: `PUT /users/me/password`
- **우선순위**: P1
- **인증**: 필요

### Request

#### Request Syntax
```bash
curl -X PUT https://api.woorido.com/api/v1/users/me/password \
  -H "Authorization: Bearer {accessToken}" \
  -H "Content-Type: application/json" \
  -d '{
    "currentPassword": "Password123!",
    "newPassword": "NewPassword456!",
    "newPasswordConfirm": "NewPassword456!"
  }'
```

#### Request Body
| 필드 | 타입 | 필수 | 제약조건 | 설명 |
|------|------|------|----------|------|
| currentPassword | String | Y | - | 현재 비밀번호 |
| newPassword | String | Y | 8-20자, 영문+숫자+특수문자 | 새 비밀번호 |
| newPasswordConfirm | String | Y | newPassword와 일치 | 새 비밀번호 확인 |

### Response

#### Response 200 OK
```json
{
  "success": true,
  "data": {
    "passwordChanged": true
  },
  "message": "비밀번호가 변경되었습니다",
  "timestamp": "2026-01-14T10:30:00Z"
}
```

### Errors
| HTTP | 코드 | 메시지 |
|------|------|--------|
| 400 | USER_003 | 현재 비밀번호가 일치하지 않습니다 |
| 400 | VALIDATION_001 | 비밀번호 형식이 올바르지 않습니다 |

---

## 012. 회원 탈퇴

### 기본 정보
- **Endpoint**: `DELETE /users/me`
- **우선순위**: P1
- **인증**: 필요

### Request

#### Request Syntax
```bash
curl -X DELETE https://api.woorido.com/api/v1/users/me \
  -H "Authorization: Bearer {accessToken}" \
  -H "Content-Type: application/json" \
  -d '{
    "password": "Password123!",
    "reason": "서비스 이용을 더 이상 하지 않습니다"
  }'
```

#### Request Body
| 필드 | 타입 | 필수 | 설명 |
|------|------|------|------|
| password | String | Y | 비밀번호 확인 |
| reason | String | N | 탈퇴 사유 |

### Response

#### Response 200 OK
```json
{
  "success": true,
  "data": {
    "userId": 1,
    "status": "WITHDRAWN",
    "withdrawnAt": "2026-01-14T10:30:00Z",
    "dataDeletedAt": "2026-02-14T10:30:00Z"
  },
  "message": "탈퇴 처리되었습니다. 30일 내 재가입 시 데이터가 복구됩니다.",
  "timestamp": "2026-01-14T10:30:00Z"
}
```

### 비즈니스 규칙
- 활성 챌린지 참여 중인 경우 먼저 탈퇴 필요
- 미정산 크레딧이 있는 경우 출금 또는 환불 필요
- 30일간 Soft Delete 상태 유지 후 완전 삭제

### Errors
| HTTP | 코드 | 메시지 |
|------|------|--------|
| 400 | USER_003 | 비밀번호가 일치하지 않습니다 |
| 400 | USER_008 | 활성 챌린지에서 먼저 탈퇴해주세요 |
| 400 | USER_009 | 미정산 크레딧이 있습니다 |

---

## 013. 사용자 조회

### 기본 정보
- **Endpoint**: `GET /users/{userId}`
- **우선순위**: P1
- **인증**: 필요

### Request

#### Request Syntax
```bash
curl -X GET https://api.woorido.com/api/v1/users/2 \
  -H "Authorization: Bearer {accessToken}"
```

#### Path Parameters
| 파라미터 | 타입 | 필수 | 설명 |
|----------|------|------|------|
| userId | Long | Y | 사용자 ID |

### Response

#### Response 200 OK
```json
{
  "success": true,
  "data": {
    "userId": 2,
    "nickname": "김철수",
    "profileImage": "https://cdn.woorido.com/profiles/2.jpg",
    "brix": 78.5,
    "stats": {
      "completedChallenges": 5,
      "totalMeetings": 30
    },
    "commonChallenges": [
      {
        "challengeId": 1,
        "name": "책벌레들"
      }
    ],
    "isVerified": true,
    "createdAt": "2025-03-15T10:00:00Z"
  },
  "timestamp": "2026-01-14T10:30:00Z"
}
```

### Errors
| HTTP | 코드 | 메시지 |
|------|------|--------|
| 404 | USER_001 | 사용자를 찾을 수 없습니다 |

---

## 014. 닉네임 중복 체크

### 기본 정보
- **Endpoint**: `GET /users/check-nickname`
- **우선순위**: P0
- **인증**: 불필요

### Request

#### Request Syntax
```bash
curl -X GET "https://api.woorido.com/api/v1/users/check-nickname?nickname=새닉네임"
```

#### Query Parameters
| 파라미터 | 타입 | 필수 | 제약조건 | 설명 |
|----------|------|------|----------|------|
| nickname | String | Y | 2-20자 | 확인할 닉네임 |

### Response

#### Response 200 OK (사용 가능)
```json
{
  "success": true,
  "data": {
    "nickname": "새닉네임",
    "available": true
  },
  "message": "사용 가능한 닉네임입니다",
  "timestamp": "2026-01-14T10:30:00Z"
}
```

#### Response 200 OK (사용 불가)
```json
{
  "success": true,
  "data": {
    "nickname": "홍길동",
    "available": false
  },
  "message": "이미 사용 중인 닉네임입니다",
  "timestamp": "2026-01-14T10:30:00Z"
}
```

### Errors
| HTTP | 코드 | 메시지 |
|------|------|--------|
| 400 | USER_006 | 닉네임은 2-20자여야 합니다 |

---

**관련 문서:**
- [00_API_OVERVIEW.md](./00_API_OVERVIEW.md) - 공통 사항
- [01_API_AUTH.md](./01_API_AUTH.md) - 인증 API
- [03_API_ACCOUNT.md](./03_API_ACCOUNT.md) - 어카운트 API
