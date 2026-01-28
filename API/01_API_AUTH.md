# AUTH API

> **총 API**: 8개
> **담당**: Spring Boot
> **관련 테이블**: `users`, `sessions`

---

## 엔드포인트 목록

| # | Method | Endpoint | 설명 | 우선순위 | 인증 |
|---|--------|----------|------|---------|------|
| 001 | POST | /auth/signup | 회원가입 | P0 | 불필요 |
| 002 | POST | /auth/login | 로그인 | P0 | 불필요 |
| 003 | POST | /auth/logout | 로그아웃 | P0 | 필요 |
| 004 | POST | /auth/refresh | 토큰 갱신 | P0 | 불필요 |
| 005 | POST | /auth/email/verify | 인증 코드 발송 | P1 | 불필요 |
| 006 | POST | /auth/email/confirm | 인증 코드 확인 | P1 | 불필요 |
| 007 | POST | /auth/password/reset | 비밀번호 재설정 요청 | P0 | 불필요 |
| 008 | PUT | /auth/password/reset | 비밀번호 재설정 실행 | P1 | 불필요 |

---

## 001. 회원가입

### 기본 정보
- **Endpoint**: `POST /auth/signup`
- **우선순위**: P0
- **인증**: 불필요

### Request

#### Request Syntax
```bash
curl -X POST https://api.woorido.com/api/v1/auth/signup \
  -H "Content-Type: application/json" \
  -d '{
    "email": "user@example.com",
    "password": "Password123!",
    "passwordConfirm": "Password123!",
    "nickname": "홍길동",
    "phone": "010-1234-5678",
    "birthDate": "1990-01-15",
    "agreedTerms": true,
    "agreedPrivacy": true,
    "agreedMarketing": false
  }'
```

#### Request Body
| 필드 | 타입 | 필수 | 제약조건 | 설명 |
|------|------|------|----------|------|
| email | String | Y | 이메일 형식 | 이메일 주소 |
| password | String | Y | 8-20자, 영문+숫자+특수문자 | 비밀번호 |
| passwordConfirm | String | Y | password와 일치 | 비밀번호 확인 |
| nickname | String | Y | 2-20자 | 닉네임 |
| phone | String | Y | 010-XXXX-XXXX | 휴대폰 번호 |
| birthDate | String | Y | YYYY-MM-DD | 생년월일 |
| agreedTerms | Boolean | Y | true | 이용약관 동의 |
| agreedPrivacy | Boolean | Y | true | 개인정보 동의 |
| agreedMarketing | Boolean | N | - | 마케팅 동의 |

### Response

#### Response 201 Created
```json
{
  "success": true,
  "data": {
    "userId": 1,
    "email": "user@example.com",
    "nickname": "홍길동",
    "createdAt": "2026-01-14T10:30:00Z"
  },
  "message": "회원가입이 완료되었습니다",
  "timestamp": "2026-01-14T10:30:00Z"
}
```

#### Response Elements
| 필드 | 타입 | 설명 |
|------|------|------|
| data.userId | Long | 사용자 ID |
| data.email | String | 이메일 |
| data.nickname | String | 닉네임 |
| data.createdAt | String | 생성일시 (ISO 8601) |

### Errors
| HTTP | 코드 | 메시지 |
|------|------|--------|
| 400 | VALIDATION_001 | 비밀번호는 8-20자, 영문+숫자+특수문자 포함 |
| 409 | USER_002 | 이미 존재하는 이메일입니다 |

---

## 002. 로그인

### 기본 정보
- **Endpoint**: `POST /auth/login`
- **우선순위**: P0
- **인증**: 불필요

### Request

#### Request Syntax
```bash
curl -X POST https://api.woorido.com/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "email": "user@example.com",
    "password": "Password123!"
  }'
```

#### Request Body
| 필드 | 타입 | 필수 | 설명 |
|------|------|------|------|
| email | String | Y | 이메일 주소 |
| password | String | Y | 비밀번호 |

### Response

#### Response 200 OK
```json
{
  "success": true,
  "data": {
    "accessToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
    "refreshToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
    "tokenType": "Bearer",
    "expiresIn": 3600,
    "user": {
      "userId": 1,
      "email": "user@example.com",
      "nickname": "홍길동",
      "profileImage": "https://cdn.woorido.com/profiles/1.jpg",
      "status": "ACTIVE",
      "isNewUser": false
    }
  },
  "timestamp": "2026-01-14T10:30:00Z"
}
```

#### Response Elements
| 필드 | 타입 | 설명 |
|------|------|------|
| data.accessToken | String | 액세스 토큰 (1시간 유효) |
| data.refreshToken | String | 리프레시 토큰 (14일 유효) |
| data.tokenType | String | 토큰 타입 (Bearer) |
| data.expiresIn | Integer | 만료 시간 (초) |
| data.user | Object | 사용자 정보 |

### Errors
| HTTP | 코드 | 메시지 |
|------|------|--------|
| 401 | AUTH_001 | 이메일 또는 비밀번호가 일치하지 않습니다 |
| 403 | USER_005 | 탈퇴 대기 상태입니다 |

---

## 003. 로그아웃

### 기본 정보
- **Endpoint**: `POST /auth/logout`
- **우선순위**: P0
- **인증**: 필요

### Request

#### Request Syntax
```bash
curl -X POST https://api.woorido.com/api/v1/auth/logout \
  -H "Authorization: Bearer {accessToken}" \
  -H "Content-Type: application/json" \
  -d '{
    "refreshToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
  }'
```

#### Request Body
| 필드 | 타입 | 필수 | 설명 |
|------|------|------|------|
| refreshToken | String | Y | 리프레시 토큰 |

### Response

#### Response 200 OK
```json
{
  "success": true,
  "data": {
    "loggedOut": true
  },
  "message": "로그아웃되었습니다",
  "timestamp": "2026-01-14T10:30:00Z"
}
```

### Errors
| HTTP | 코드 | 메시지 |
|------|------|--------|
| 401 | AUTH_001 | 인증이 필요합니다 |

---

## 004. 토큰 갱신

### 기본 정보
- **Endpoint**: `POST /auth/refresh`
- **우선순위**: P0
- **인증**: 불필요

### Request

#### Request Syntax
```bash
curl -X POST https://api.woorido.com/api/v1/auth/refresh \
  -H "Content-Type: application/json" \
  -d '{
    "refreshToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
  }'
```

#### Request Body
| 필드 | 타입 | 필수 | 설명 |
|------|------|------|------|
| refreshToken | String | Y | 리프레시 토큰 |

### Response

#### Response 200 OK
```json
{
  "success": true,
  "data": {
    "accessToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
    "refreshToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
    "tokenType": "Bearer",
    "expiresIn": 3600
  },
  "timestamp": "2026-01-14T10:30:00Z"
}
```

### Errors
| HTTP | 코드 | 메시지 |
|------|------|--------|
| 401 | AUTH_004 | 리프레시 토큰이 만료되었습니다 |

---

## 005. 인증 코드 발송

### 기본 정보
- **Endpoint**: `POST /auth/email/verify`
- **우선순위**: P1
- **인증**: 불필요

### Request

#### Request Syntax
```bash
curl -X POST https://api.woorido.com/api/v1/auth/email/verify \
  -H "Content-Type: application/json" \
  -d '{
    "email": "user@example.com",
    "type": "SIGNUP"
  }'
```

#### Request Body
| 필드 | 타입 | 필수 | 제약조건 | 설명 |
|------|------|------|----------|------|
| email | String | Y | 이메일 형식 | 이메일 주소 |
| type | String | Y | SIGNUP / PASSWORD_RESET | 발송 목적 |

### Response

#### Response 200 OK
```json
{
  "success": true,
  "data": {
    "email": "user@example.com",
    "expiresIn": 300,
    "retryAfter": 60
  },
  "message": "인증 코드가 발송되었습니다",
  "timestamp": "2026-01-14T10:30:00Z"
}
```

### Errors
| HTTP | 코드 | 메시지 |
|------|------|--------|
| 400 | AUTH_007 | 잠시 후 다시 시도해주세요 |
| 409 | USER_002 | 이미 존재하는 이메일입니다 |

---

## 006. 인증 코드 확인

### 기본 정보
- **Endpoint**: `POST /auth/email/confirm`
- **우선순위**: P1
- **인증**: 불필요

### Request

#### Request Syntax
```bash
curl -X POST https://api.woorido.com/api/v1/auth/email/confirm \
  -H "Content-Type: application/json" \
  -d '{
    "email": "user@example.com",
    "code": "123456"
  }'
```

#### Request Body
| 필드 | 타입 | 필수 | 제약조건 | 설명 |
|------|------|------|----------|------|
| email | String | Y | 이메일 형식 | 이메일 주소 |
| code | String | Y | 6자리 숫자 | 인증 코드 |

### Response

#### Response 200 OK
```json
{
  "success": true,
  "data": {
    "email": "user@example.com",
    "verified": true,
    "verificationToken": "temp_token_for_signup_or_reset"
  },
  "message": "이메일이 인증되었습니다",
  "timestamp": "2026-01-14T10:30:00Z"
}
```

### Errors
| HTTP | 코드 | 메시지 |
|------|------|--------|
| 400 | AUTH_006 | 인증 코드가 일치하지 않습니다 |
| 400 | AUTH_008 | 인증 코드가 만료되었습니다 |

---

## 007. 비밀번호 재설정 요청

### 기본 정보
- **Endpoint**: `POST /auth/password/reset`
- **우선순위**: P0
- **인증**: 불필요

### Request

#### Request Syntax
```bash
curl -X POST https://api.woorido.com/api/v1/auth/password/reset \
  -H "Content-Type: application/json" \
  -d '{
    "email": "user@example.com"
  }'
```

#### Request Body
| 필드 | 타입 | 필수 | 설명 |
|------|------|------|------|
| email | String | Y | 가입된 이메일 |

### Response

#### Response 200 OK
```json
{
  "success": true,
  "data": {
    "email": "user@example.com",
    "expiresIn": 1800
  },
  "message": "비밀번호 재설정 링크가 발송되었습니다",
  "timestamp": "2026-01-14T10:30:00Z"
}
```

### Errors
| HTTP | 코드 | 메시지 |
|------|------|--------|
| 404 | USER_001 | 사용자를 찾을 수 없습니다 |

---

## 008. 비밀번호 재설정 실행

### 기본 정보
- **Endpoint**: `PUT /auth/password/reset`
- **우선순위**: P1
- **인증**: 불필요

### Request

#### Request Syntax
```bash
curl -X PUT https://api.woorido.com/api/v1/auth/password/reset \
  -H "Content-Type: application/json" \
  -d '{
    "token": "reset_token_from_email",
    "newPassword": "NewPassword123!",
    "newPasswordConfirm": "NewPassword123!"
  }'
```

#### Request Body
| 필드 | 타입 | 필수 | 제약조건 | 설명 |
|------|------|------|----------|------|
| token | String | Y | - | 재설정 토큰 |
| newPassword | String | Y | 8-20자, 영문+숫자+특수문자 | 새 비밀번호 |
| newPasswordConfirm | String | Y | newPassword와 일치 | 새 비밀번호 확인 |

### Response

#### Response 200 OK
```json
{
  "success": true,
  "data": {
    "passwordReset": true
  },
  "message": "비밀번호가 재설정되었습니다",
  "timestamp": "2026-01-14T10:30:00Z"
}
```

### Errors
| HTTP | 코드 | 메시지 |
|------|------|--------|
| 400 | AUTH_009 | 재설정 토큰이 만료되었습니다 |
| 400 | VALIDATION_001 | 비밀번호 형식이 올바르지 않습니다 |

---

**관련 문서:**
- [00_API_OVERVIEW.md](./00_API_OVERVIEW.md) - 공통 사항
- [02_API_USER.md](./02_API_USER.md) - 사용자 API
