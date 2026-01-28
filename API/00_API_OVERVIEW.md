# WOORIDO API 명세서 - Overview

> **Version:** v1.0.0
> **Last Updated:** 2026-01-23
> **총 API:** 92개 (Spring 83 + Django 9)
> **기준 문서:** API_SCHEMA_1.0.0.md, POLICY_DEFINITION.md, DB_Schema_1.0.0.md

---

## 1. 도메인별 API 문서

| # | 도메인 | 파일 | API 수 | 담당 |
|---|--------|------|--------|------|
| 01 | 인증 | [01_API_AUTH.md](./01_API_AUTH.md) | 8개 | Spring |
| 02 | 사용자 | [02_API_USER.md](./02_API_USER.md) | 6개 | Spring |
| 03 | 어카운트 | [03_API_ACCOUNT.md](./03_API_ACCOUNT.md) | 7개 | Spring |
| 04 | 챌린지 | [04_API_CHALLENGE.md](./04_API_CHALLENGE.md) | 13개 | Spring |
| 05 | 모임 | [05_API_MEETING.md](./05_API_MEETING.md) | 7개 | Spring |
| 06 | 투표 | [06_API_VOTE.md](./06_API_VOTE.md) | 5개 | Spring |
| 07 | SNS | [07_API_SNS.md](./07_API_SNS.md) | 17개 | Spring |
| 08 | 시스템 | [08_API_SYSTEM.md](./08_API_SYSTEM.md) | 12개 | Spring |
| 09 | **장부** | [09_API_LEDGER.md](./09_API_LEDGER.md) | **5개** | Spring |
| 10 | Django | [10_API_DJANGO.md](./10_API_DJANGO.md) | 9개 | Django |

---

## 2. 공통 사항

### 2.1 기본 정보

| 항목 | 값 |
|------|-----|
| Base URL | `https://api.woorido.com/api/v1` |
| Content-Type | `application/json` |
| Encoding | UTF-8 |

### 2.2 인증 헤더

| 헤더 | 필수 | 값 | 설명 |
|------|------|-----|------|
| Authorization | Y | `Bearer {accessToken}` | 액세스 토큰 |
| Content-Type | Y | `application/json` | 요청 본문 타입 |
| X-Device-Id | N | `{deviceId}` | 기기 고유 ID |
| X-App-Version | N | `1.0.0` | 앱 버전 |
| X-Platform | N | `IOS` / `ANDROID` / `WEB` | 플랫폼 |

### 2.3 공통 응답 형식

**성공 응답:**
```json
{
  "success": true,
  "data": { },
  "message": "요청이 성공했습니다",
  "timestamp": "2026-01-14T10:30:00Z"
}
```

**에러 응답:**
```json
{
  "success": false,
  "error": {
    "code": "ERROR_CODE",
    "message": "에러 메시지",
    "details": { }
  },
  "timestamp": "2026-01-14T10:30:00Z"
}
```

### 2.4 페이지네이션

**요청 파라미터:**
| 파라미터 | 타입 | 기본값 | 설명 |
|----------|------|--------|------|
| page | Integer | 0 | 페이지 번호 (0부터 시작) |
| size | Integer | 20 | 페이지 크기 (최대 100) |
| sort | String | - | 정렬 (예: `createdAt,desc`) |

**응답 형식:**
```json
{
  "content": [ ],
  "page": {
    "number": 0,
    "size": 20,
    "totalElements": 100,
    "totalPages": 5
  }
}
```

### 2.5 HTTP 상태 코드

| 코드 | 설명 | 사용 상황 |
|------|------|----------|
| 200 | OK | 조회/수정 성공 |
| 201 | Created | 생성 성공 |
| 204 | No Content | 삭제 성공 |
| 400 | Bad Request | 유효성 검증 실패 |
| 401 | Unauthorized | 인증 필요/실패 |
| 403 | Forbidden | 권한 없음 |
| 404 | Not Found | 리소스 없음 |
| 409 | Conflict | 중복/충돌 |
| 500 | Internal Server Error | 서버 오류 |

---

## 3. 보안 요구사항

### 3.1 인증 방식
- **방식:** JWT (JSON Web Token)
- **Access Token 만료:** 1시간
- **Refresh Token 만료:** 14일
- **헤더 형식:** `Authorization: Bearer {accessToken}`
- **사용자 권한 (Roles):**
  - `ROLE_USER`: 일반 사용자 (토큰 발급 시 기본)
  - `ROLE_ADMIN`: 시스템 관리자 (정산/환불 처리 권한)

### 3.2 비밀번호 정책
- **최소/최대 길이:** 8-20자
- **필수 조합:** 영문 + 숫자 + 특수문자
- **허용 특수문자:** `!@#$%^&*()_+-=[]{}|;:,.<>?`

### 3.3 API Rate Limiting

| 구분 | 제한 | 기준 |
|------|------|------|
| 일반 API | 100 요청/분 | IP 기준 |
| 인증 API | 10 요청/분 | IP 기준 |
| 검색 API | 30 요청/분 | 사용자 기준 |
| 파일 업로드 | 10 요청/시간 | 사용자 기준 |

### 3.4 민감 정보 처리

| 항목 | 처리 방식 |
|------|----------|
| 비밀번호 | BCrypt 해싱 (cost factor: 12) |
| 전화번호 | AES-256 암호화 저장 |
| 계좌번호 | AES-256 암호화 저장, 마스킹 표시 |

---

## 4. 우선순위 정의

| 우선순위 | 설명 | 예시 |
|----------|------|------|
| **P0** | MVP 필수 기능 | 회원가입, 로그인, 챌린지 CRUD |
| **P1** | 핵심 부가 기능 | 투표, 지출 관리, 알림 |
| **P2** | 편의 기능 | 파일 업로드, 장부 내보내기 |
| **P3** | 고급 기능 (차후) | AI 추천, 리포트 생성 |

---

**관련 문서:**
- [API_SPECIFICATION_1.0.0.md](../API_SPECIFICATION_1.0.0.md) - 원본 통합 문서
- [API_SCHEMA_1.0.0.md](../API_SCHEMA_1.0.0.md) - API 스키마
- [DB_Schema_1.0.0.md](../DB_Schema_1.0.0.md) - 데이터베이스 스키마
