# WooriDo 개발 컨벤션 (Development Convention)

> **목적**: WooriDo 프로젝트의 코드 일관성, 유지보수성, 그리고 **Vibe Coding** 효율성을 극대화하기 위한 표준을 정의합니다.
> **적용 대상**: 프론트엔드(React), 백엔드(Spring Boot, Django), 데이터베이스(Oracle)

---

## 1. 핵심 철학: Vibe Coding Strategy
> 참조: [00_VIBE_CODING_STRATEGY.md](../../01_PLANNING/00_VIBE_CODING_STRATEGY.md)

*   **참조(Reference) vs 실행(Executable) 분리**:
    *   **Docs (`/docs`)**: 비즈니스 로직, 정책, API 명세, 데이터 구조 등 **"What & Why"**를 기록합니다.
    *   **Skills (`SKILL.md`)**: 구현 패턴, 코드 템플릿, 유틸리티 등 **"How"**를 기록합니다.
*   **Case-by-Case 지양, Rule-based 지향**:
    *   개별 API마다 구현 방법을 나열하지 않고, **제네릭 패턴(Generic Patterns)**(통신, 에러 처리, UI 등)을 정의하여 적용합니다.

---

## 2. 네이밍 컨벤션 및 용어 (Naming & Terminology)
> 참조: [TERMINOLOGY.md](../../01_PLANNING/Product/TERMINOLOGY.md)

### 2.1 도메인 용어 (Strict Enforcement)
법적 리스크 회피 및 로직 명확성을 위해 아래 용어를 엄격히 준수합니다.

| 카테고리 | ⛔ 금지어 (사용 지양) | ✅ 표준어 (사용 권장) | 비고 |
| :--- | :--- | :--- | :--- |
| **모임** | 계모임, 곗돈 | **Challenge (챌린지)** | 금융법 저촉 방지 |
| **역할** | 계주, 계원 | **Leader (리더)**, **Follower (팔로워)** | 관리 주체 명확화 |
| **행위** | 회비 납부, 투자 | **Support (서포트)** | 팬덤/후원 개념 적용 |
| **지출** | 지출 요청/승인 | **Open (오픈)** | "지출에 대해 투표를 연다"는 의미 |
| **자산** | 리워드, 수익금 | **Benefit (베네핏)** | 리더 혜택 |
| **화폐** | - | **Credit (크레딧)** | 플랫폼 가상 화폐 |

### 2.2 코드 네이밍 규칙
*   **기본 스타일**: `camelCase`를 기본으로 사용합니다.
*   **파일/폴더**:
    *   React 컴포넌트: `PascalCase` (예: `UserCard.tsx`)
    *   Hook/유틸리티: `camelCase` (예: `useAuth.ts`, `formatDate.ts`)
*   **DB 컬럼**: `snake_case` (Oracle DB 호환성)
*   **변수명**:
    *   Boolean: `is`, `has`, `can`, `should` 접두사 (예: `isVisible`, `hasData`)
    *   핸들러: `handle` (함수 정의), `on` (props 전달) (예: `handleClick`, `onSubmit`)
*   **명확성**: 모호한 축약어는 금지하며, 풀네임을 사용하여 의미를 명확히 합니다.

---

## 3. 아키텍처 표준 (Architecture Standards)
> 참조: [SYSTEM_ARCHITECTURE.md](../Architecture/SYSTEM_ARCHITECTURE.md)

### 3.1 3-Tier & 역할 분담
*   **Frontend**: React (UI/UX)
*   **Main Backend**: Spring Boot (Business Logic, WRITE Operations)
*   **Sub Backend**: Django (Read/Compute Only, Analysis)
*   **Database**: Oracle (Main Data Store)

### 3.2 Django 제약 사항 (Critical)
*   **Django는 Oracle DB에 직접 쓰기(INSERT/UPDATE/DELETE)를 수행할 수 없습니다.**
*   모든 데이터 변경은 **Spring Boot API**를 통해 수행해야 합니다.
*   Django 허용 범위:
    *   Spring Boot로부터 데이터 수신 (Webhook/Sync)
    *   Elasticsearch 검색 및 인덱싱
    *   통계 분석 및 추천 알고리즘 계산

---

## 4. 프론트엔드 컨벤션 (Frontend - WDS)
> 참조: [DESIGN_TOKENS.md](../Frontend/DesignSystem/DESIGN_TOKENS.md)

### 4.1 디자인 토큰 필수 사용
*   **Hardcoded Hex Value 금지**: 색상 코드를 직접 입력하지 마십시오.
*   **사용 예시**:
    *   ❌ `color: '#E9481E'`
    *   ✅ `color: tokens.colors.orange500`
    *   ✅ `background: tokens.colors.background`

### 4.2 타이포그래피 및 금융 데이터
*   텍스트 크기는 `w1` ~ `w7` 토큰을 사용합니다.
*   **금액 표시**: 반드시 `financial-*` 토큰과 `tabular-nums` 속성을 사용하여 숫자가 흔들리지 않도록 합니다.

### 4.3 컴포넌트 구조
*   **Props Interface**: 모든 컴포넌트는 `Props` 인터페이스를 명시적으로 정의해야 합니다.
*   **분류**: `Foundation`, `Domain` (비즈니스 로직 포함), `Ledger`, `Overlay` 등으로 명확히 구분합니다.

---

## 5. 백엔드 및 데이터베이스 패턴
> 참조: [IMPLEMENTATION_PATTERNS.md](../Database/07_IMPLEMENTATION_PATTERNS.md)

### 5.1 트랜잭션 및 락(Lock)
*   **동시성 제어**: `version` 컬럼을 이용한 **낙관적 락(Optimistic Locking)**을 기본으로 합니다.
*   **잔액 갱신**: 금전 관련 로직은 **비관적 락(Pessimistic Locking, `SFU`)**을 사용하여 정합성을 보장합니다.

### 5.2 삭제 정책
*   **Soft Delete**: `deleted_at` 컬럼 업데이트를 기본으로 하며, 물리적 삭제(Hard Delete)는 지양합니다.

### 5.3 Oracle 호환성
*   **타입 매핑**:
    *   `BIGINT` ➡️ `NUMBER(19)`
    *   `VARCHAR` ➡️ `VARCHAR2`
    *   UUID ➡️ `VARCHAR2(36)` (또는 RAW)

---

## 6. 코드 작성 컨벤션 (Code Writing Standards)
> 참고: [Naver Blog - Clean Code & Git Convention](https://m.blog.naver.com/so_no7/223507064379)

### 6.1 커밋 메시지 (Commit Message)
*   **구조**:
    ```text
    타입: 제목

    본문

    꼬리말
    ```
*   **타입 (Type)**:
    *   `Feat`: 새로운 기능 추가
    *   `Fix`: 버그 수정
    *   `Docs`: 문서 수정
    *   `Style`: 코드 포맷팅 (로직 변경 없음)
    *   `Refactor`: 코드 리팩토링 (기능 변경 없음)
    *   `Test`: 테스트 코드 추가/수정
    *   `Chore`: 빌드, 패키지 매니저 설정 등
*   **작성 규칙**:
    *   제목: 50자 이내, 첫 글자 대문자, 마침표(.) 사용 금지, **명령문/현재형** (예: "Add user login")
    *   본문: **"What"**과 **"Why"**를 설명 (How는 코드에 위임)

### 6.2 자바스크립트/타입스크립트 스타일
*   **ES6+**: `var` 사용 금지 (`let`, `const` 사용).
*   **Immutability**: 기본적으로 `const`를 사용하고, 재할당이 필요한 경우에만 `let`을 사용합니다.
*   **함수형 프로그래밍**: 부작용(Side-effect)을 최소화하고 순수 함수를 지향합니다.

---

**최종 업데이트**: 2026-01-21
**승인**: WooriDo Tech Lead
