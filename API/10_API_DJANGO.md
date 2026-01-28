# DJANGO API (검색/분석/추천)

> **총 API**: 9개 (검색 3개 + 분석 4개 + 추천 2개)
> **담당**: Django (Python)
> **특징**: AI 기반 검색, 통계 분석, 맞춤 추천

---

## ⚠️ 아키텍처 주의사항

> [!IMPORTANT]
> **Django는 DB와 직접 소통하지 않습니다.**
>
> 모든 데이터 조회/저장은 **Spring (Oracle)** 을 통해서만 이루어집니다.

### 데이터 흐름

```
┌──────────┐      ┌──────────┐      ┌──────────┐      ┌──────────┐
│   FE     │ ──▶  │  Spring  │ ──▶  │  Django  │ ──▶  │  Spring  │ ──▶ DB
│ (React)  │      │  (Main)  │      │  (AI/ML) │      │  (Data)  │
└──────────┘      └──────────┘      └──────────┘      └──────────┘
                        │                 │
                        │  프록시 요청    │  분석 결과
                        └─────────────────┘
```

### 흐름 설명
1. **FE → Spring**: 클라이언트 요청 수신
2. **Spring → Django**: AI/분석 작업 위임 (내부 API 호출)
3. **Django → Spring**: 필요 데이터 요청 (Spring Internal API)
4. **Django → Spring**: 분석 결과 반환
5. **Spring → FE**: 최종 응답 전달

### 관련 Spring 내부 API
Django가 호출하는 Spring 내부 API:
- `GET /internal/challenges` - 챌린지 데이터 조회
- `GET /internal/users` - 사용자 데이터 조회
- `GET /internal/transactions` - 거래 데이터 조회

---

## 엔드포인트 목록

### 검색 (SEARCH)

| # | Method | Endpoint | 설명 | 우선순위 | 인증 |
|---|--------|----------|------|---------|------|
| 083 | GET | /search | 통합 검색 | P1 | 필요 |
| 084 | GET | /search/challenges | 챌린지 검색 | P1 | 필요 |
| 085 | GET | /search/autocomplete | 검색어 자동완성 | P2 | 필요 |

### 통계/분석 (ANALYTICS)

| # | Method | Endpoint | 설명 | 우선순위 | 인증 |
|---|--------|----------|------|---------|------|
| 086 | GET | /analytics/user/activity | 사용자 활동 통계 | P2 | 필요 |
| 087 | GET | /analytics/challenge/{challengeId} | 챌린지 분석 | P2 | 멤버 |
| 088 | GET | /analytics/dashboard | 전체 통계 대시보드 | P2 | 필요 |
| 089 | GET | /analytics/user/report | 개인 리포트 생성 | P3 | 필요 |

### 추천 (RECOMMENDATION)

| # | Method | Endpoint | 설명 | 우선순위 | 인증 |
|---|--------|----------|------|---------|------|
| 090 | GET | /recommendations/challenges | 챌린지 추천 | P2 | 필요 |
| 091 | GET | /recommendations/savings-plan | 맞춤형 저축 플랜 추천 | P3 | 필요 |

---

## 083. 통합 검색

### 기본 정보
- **Endpoint**: `GET /search`
- **우선순위**: P1
- **인증**: 필요
- **서버**: Django

### Request

#### Request Syntax
```bash
curl -X GET "https://api.woorido.com/api/v1/search?q=저축&type=ALL&page=0&size=20" \
  -H "Authorization: Bearer {accessToken}"
```

#### Query Parameters
| 파라미터 | 타입 | 필수 | 기본값 | 설명 |
|----------|------|------|--------|------|
| q | String | Y | - | 검색어 (2자 이상) |
| type | String | N | - | ALL / CHALLENGE / POST / USER |
| page | Integer | N | 0 | 페이지 번호 |
| size | Integer | N | 20 | 페이지 크기 |

### Response

#### Response 200 OK
```json
{
  "success": true,
  "data": {
    "query": "저축",
    "totalResults": 35,
    "challenges": {
      "items": [
        {
          "challengeId": 1,
          "title": "2026 저축 챌린지",
          "description": "함께 저축하는 모임입니다...",
          "status": "IN_PROGRESS",
          "currentMembers": 10
        }
      ],
      "total": 5
    },
    "posts": {
      "items": [
        {
          "postId": 24,
          "title": "저축 꿀팁 공유합니다!",
          "challengeTitle": "2026 저축 챌린지",
          "author": "박저축"
        }
      ],
      "total": 25
    },
    "users": {
      "items": [
        {
          "userId": 3,
          "nickname": "박저축",
          "profileImage": "https://cdn.woorido.com/profiles/3.jpg"
        }
      ],
      "total": 5
    }
  },
  "timestamp": "2026-01-14T10:30:00Z"
}
```

### Errors
| HTTP | 코드 | 메시지 |
|------|------|--------|
| 400 | SEARCH_001 | 검색어는 2자 이상 입력해주세요 |

---

## 081. 챌린지 검색

### 기본 정보
- **Endpoint**: `GET /search/challenges`
- **우선순위**: P1
- **인증**: 필요
- **서버**: Django

### Request

#### Query Parameters
| 파라미터 | 타입 | 필수 | 기본값 | 설명 |
|----------|------|------|--------|------|
| q | String | Y | - | 검색어 |
| category | String | N | - | HOBBY/STUDY/EXERCISE/SAVINGS/TRAVEL/FOOD/CULTURE/OTHER |
| status | String | N | - | RECRUITING/IN_PROGRESS/COMPLETED |
| minDeposit | Long | N | - | 최소 월 납입금 |
| maxDeposit | Long | N | - | 최대 월 납입금 |
| minMembers | Integer | N | - | 최소 인원 |
| maxMembers | Integer | N | - | 최대 인원 |
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
        "title": "2026 저축 챌린지",
        "description": "함께 저축하는 모임입니다. 매달 5만원씩 저축해요!",
        "category": "SAVING",
        "status": "IN_PROGRESS",
        "leaderNickname": "홍길동",
        "currentMembers": 10,
        "maxMembers": 20,
        "monthlyDeposit": 50000,
        "startDate": "2026-01-01",
        "endDate": "2026-12-31",
        "relevanceScore": 95.5
      }
    ],
    "totalElements": 12,
    "totalPages": 1,
    "number": 0,
    "size": 20
  },
  "timestamp": "2026-01-14T10:30:00Z"
}
```

---

## 082. 검색어 자동완성

### 기본 정보
- **Endpoint**: `GET /search/autocomplete`
- **우선순위**: P2
- **인증**: 필요
- **서버**: Django

### Request

#### Query Parameters
| 파라미터 | 타입 | 필수 | 기본값 | 설명 |
|----------|------|------|--------|------|
| q | String | Y | - | 검색어 (1자 이상) |
| type | String | N | - | CHALLENGE / POST / USER |
| limit | Integer | N | 10 | 결과 수 (최대 20) |

### Response

#### Response 200 OK
```json
{
  "success": true,
  "data": {
    "suggestions": [
      {
        "text": "저축 챌린지",
        "type": "CHALLENGE",
        "id": 1,
        "score": 98.5
      },
      {
        "text": "저축 꿀팁",
        "type": "POST",
        "id": 24,
        "score": 85.2
      },
      {
        "text": "박저축",
        "type": "USER",
        "id": 3,
        "score": 72.0
      }
    ]
  },
  "timestamp": "2026-01-14T10:30:00Z"
}
```

---

## 083. 사용자 활동 통계

### 기본 정보
- **Endpoint**: `GET /analytics/user/activity`
- **우선순위**: P2
- **인증**: 필요
- **서버**: Django

### Request

#### Query Parameters
| 파라미터 | 타입 | 필수 | 기본값 | 설명 |
|----------|------|------|--------|------|
| period | String | N | MONTH | WEEK / MONTH / YEAR |

### Response

#### Response 200 OK
```json
{
  "success": true,
  "data": {
    "userId": 3,
    "period": "MONTH",
    "summary": {
      "totalDeposit": 150000,
      "challengeCount": 2,
      "completionRate": 100.0,
      "postCount": 5,
      "commentCount": 12
    },
    "dailyTrend": [
      {
        "date": "2026-01-01",
        "depositAmount": 50000,
        "activityScore": 85.0
      }
    ],
    "ranking": {
      "depositRank": 15,
      "activityRank": 8,
      "totalUsers": 1500
    }
  },
  "timestamp": "2026-01-14T10:30:00Z"
}
```

---

## 084. 챌린지 분석

### 기본 정보
- **Endpoint**: `GET /analytics/challenge/{challengeId}`
- **우선순위**: P2
- **인증**: 필요 (멤버)
- **서버**: Django

### Response

#### Response 200 OK
```json
{
  "success": true,
  "data": {
    "challengeId": 1,
    "overview": {
      "totalDeposit": 1500000,
      "averageDeposit": 150000,
      "completionRate": 85.5,
      "activeMembers": 10
    },
    "trends": {
      "depositTrend": [
        {"month": "2026-01", "amount": 500000},
        {"month": "2026-02", "amount": 520000}
      ],
      "memberTrend": [
        {"month": "2026-01", "joined": 10, "left": 0}
      ]
    },
    "memberAnalysis": [
      {
        "userId": 1,
        "nickname": "홍길동",
        "depositRate": 100.0,
        "activityScore": 95.0,
        "rank": 1
      }
    ],
    "predictions": {
      "expectedTotal": 6000000,
      "successProbability": 92.5
    }
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

## 085. 전체 통계 대시보드

### 기본 정보
- **Endpoint**: `GET /analytics/dashboard`
- **우선순위**: P2
- **인증**: 필요
- **서버**: Django

### Response

#### Response 200 OK
```json
{
  "success": true,
  "data": {
    "userStats": {
      "totalUsers": 15000,
      "activeUsers": 8500,
      "newUsers": 320
    },
    "challengeStats": {
      "totalChallenges": 850,
      "activeChallenges": 420,
      "completedRate": 78.5
    },
    "financialStats": {
      "totalDeposit": 2500000000,
      "averageDeposit": 165000,
      "totalSettled": 1800000000
    },
    "topChallenges": [
      {
        "challengeId": 1,
        "title": "2026 저축 챌린지",
        "memberCount": 45,
        "totalDeposit": 25000000
      }
    ]
  },
  "timestamp": "2026-01-14T10:30:00Z"
}
```

---

## 086. 개인 리포트 생성

### 기본 정보
- **Endpoint**: `GET /analytics/user/report`
- **우선순위**: P3
- **인증**: 필요
- **서버**: Django

### Request

#### Query Parameters
| 파라미터 | 타입 | 필수 | 기본값 | 설명 |
|----------|------|------|--------|------|
| period | String | N | MONTH | MONTH / QUARTER / YEAR |
| format | String | N | JSON | JSON / PDF |

### Response

#### Response 200 OK
```json
{
  "success": true,
  "data": {
    "reportId": "RPT_20260114_003",
    "period": "MONTH",
    "periodRange": {
      "start": "2026-01-01",
      "end": "2026-01-31"
    },
    "summary": {
      "totalDeposit": 150000,
      "totalChallenges": 2,
      "completionRate": 100.0,
      "savingsGrowth": 15.5
    },
    "achievements": [
      {
        "type": "STREAK",
        "title": "연속 납입 달성",
        "description": "3개월 연속 납입을 달성했습니다!",
        "achievedAt": "2026-01-05"
      }
    ],
    "recommendations": [
      {
        "type": "INCREASE_SAVINGS",
        "title": "저축액 증가 제안",
        "description": "현재 저축률이 좋습니다. 월 5만원 추가 저축을 고려해보세요."
      }
    ],
    "downloadUrl": "https://cdn.woorido.com/reports/RPT_20260114_003.pdf"
  },
  "timestamp": "2026-01-14T10:30:00Z"
}
```

---

## 087. 챌린지 추천

### 기본 정보
- **Endpoint**: `GET /recommendations/challenges`
- **우선순위**: P2
- **인증**: 필요
- **서버**: Django

### Request

#### Query Parameters
| 파라미터 | 타입 | 필수 | 기본값 | 설명 |
|----------|------|------|--------|------|
| limit | Integer | N | 10 | 결과 수 (최대 20) |

### Response

#### Response 200 OK
```json
{
  "success": true,
  "data": {
    "recommendations": [
      {
        "challengeId": 15,
        "title": "직장인 점심값 아끼기",
        "category": "EXPENSE_CUT",
        "matchScore": 95.5,
        "reason": "회원님의 지출 패턴과 유사한 사용자들이 많이 참여하고 있습니다",
        "currentMembers": 23,
        "maxMembers": 30,
        "monthlyDeposit": 30000,
        "startDate": "2026-02-01"
      }
    ],
    "basedOn": {
      "userPreferences": ["SAVING", "TRAVEL", "EXPENSE_CUT"],
      "similarUsers": 245,
      "algorithm": "collaborative_filtering_v2"
    }
  },
  "timestamp": "2026-01-14T10:30:00Z"
}
```

---

## 088. 맞춤형 저축 플랜 추천

### 기본 정보
- **Endpoint**: `GET /recommendations/savings-plan`
- **우선순위**: P3
- **인증**: 필요
- **서버**: Django

### Request

#### Query Parameters
| 파라미터 | 타입 | 필수 | 기본값 | 설명 |
|----------|------|------|--------|------|
| monthlyBudget | Long | N | - | 월 가용 예산 |
| goalAmount | Long | N | - | 목표 금액 |
| goalPeriod | Integer | N | - | 목표 기간 (개월) |

### Response

#### Response 200 OK
```json
{
  "success": true,
  "data": {
    "plans": [
      {
        "planId": "PLAN_A",
        "name": "안정형 저축 플랜",
        "description": "낮은 리스크로 꾸준히 저축하는 플랜입니다",
        "monthlyDeposit": 50000,
        "expectedReturn": 630000,
        "duration": 12,
        "riskLevel": "LOW",
        "matchedChallenges": [
          {
            "challengeId": 1,
            "title": "2026 저축 챌린지",
            "monthlyDeposit": 50000
          }
        ]
      },
      {
        "planId": "PLAN_B",
        "name": "적극형 저축 플랜",
        "description": "더 높은 목표를 위한 적극적인 저축 플랜입니다",
        "monthlyDeposit": 100000,
        "expectedReturn": 1320000,
        "duration": 12,
        "riskLevel": "MEDIUM"
      }
    ],
    "analysis": {
      "currentSavingRate": 15.5,
      "recommendedRate": 20.0,
      "projectedGoal": 1200000
    }
  },
  "timestamp": "2026-01-14T10:30:00Z"
}
```

---

**관련 문서:**
- [00_API_OVERVIEW.md](./00_API_OVERVIEW.md) - 공통 사항
- [08_API_SYSTEM.md](./08_API_SYSTEM.md) - 시스템 API
