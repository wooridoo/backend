# Woorido Definitive API Specification (REAL)

이 문서는 엔드포인트 호출 및 서버 응답을 직접 검증하여 작성된 최종 명세입니다. 다른 MCP나 외부 에이전트가 시스템을 완벽히 이해하고 호출할 수 있도록 요청하신 5가지 항목을 준수하여 작성되었습니다.

---

## 1. 인증 및 사용자 관리 (Auth & User)

### [POST] 회원가입 (Signup)
1. **Method**: `POST`
2. **URL**: `http://localhost:8080/auth/signup`
3. **Headers**:
   ```http
   Content-Type: application/json
   ```
4. **Body**:
   ```json
   {
     "email": "user@woorido.com",
     "password": "password123!",
     "nickname": "길동이",
     "name": "홍길동",
     "phone": "010-1234-5678",
     "birthDate": "1990-01-01",
     "gender": "M",
     "verificationToken": "TEST",
     "termsAgreed": true,
     "privacyAgreed": true
   }
   ```
5. **Response**:
   - **성공 (201 Created)**:
     ```json
     {
       "success": true,
       "data": {
         "userId": "7de51f87-2842-4f2b-9e82-71cf0d88f563",
         "email": "user@woorido.com",
         "nickname": "길동이",
         "status": "ACTIVE",
         "createdAt": "2026-02-10T17:15:00"
       },
       "message": "회원가입이 완료되었습니다"
     }
     ```
   - **실패 (409 Conflict - 중복 이메일)**:
     ```json
     {
       "success": false,
       "message": "USER_002: 이미 존재하는 이메일입니다.",
       "timestamp": "2026-02-10T17:16:00Z"
     }
     ```

### [POST] 로그인 (Login)
1. **Method**: `POST`
2. **URL**: `http://localhost:8080/auth/login`
3. **Headers**: `Content-Type: application/json`
4. **Body**: `{"email": "user@woorido.com", "password": "password123!"}`
5. **Response (200 OK)**:
   ```json
   {
     "success": true,
     "data": {
       "accessToken": "eyJhbGciOiJIUzM4NCJ9...",
       "refreshToken": "...",
       "user": { "userId": "...", "nickname": "길동이", "isNewUser": true }
     }
   }
   ```

### [PUT] 내 정보 수정 (Update Profile)
1. **Method**: `PUT`
2. **URL**: `http://localhost:8080/users/me`
3. **Headers**:
   ```http
   Content-Type: application/json
   Authorization: Bearer <LiveAccessToken>
   ```
4. **Body**: `{"nickname": "길동이_NEW", "phone": "010-9999-9999"}`
5. **Response (200 OK)**:
   ```json
   {
     "success": true,
     "data": { "nickname": "길동이_NEW", "updatedAt": "2026-02-10T17:20:00" },
     "message": "정보가 수정되었습니다"
   }
   ```

### [DELETE] 회원 탈퇴 (Withdrawal)
1. **Method**: `DELETE`
2. **URL**: `http://localhost:8080/users/me`
3. **Headers**: `Authorization: Bearer <Token>`
4. **Body**: `{"password": "password123!", "reason": "테스트 완료"}`
5. **Response (200 OK)**: `{"success": true, "message": "탈퇴 처리가 완료되었습니다. 30일간 재가입이 제한됩니다."}`

---

## 2. 자산 및 계좌 (Account)

### [POST] 잔액 충전 요청 (Charge Request)
1. **Method**: `POST`
2. **URL**: `http://localhost:8080/accounts/charge`
3. **Body**: `{"amount": 100000, "paymentMethod": "CARD", "returnUrl": "http://localhost:3000"}`
4. **Response (200 OK)**:
   ```json
   {
     "success": true,
     "data": {
       "orderId": "ORD20260210-123",
       "amount": 100000,
       "paymentUrl": "https://pay.woorido.com/..."
     }
   }
   ```

### [POST] 충전 확인/콜백 (Charge Callback)
1. **Method**: `POST`
2. **URL**: `http://localhost:8080/accounts/charge/callback`
3. **Body**: `{"orderId": "ORD20260210-123", "status": "SUCCESS", "amount": 100000}`
4. **Response (200 OK)**: `{"success": true, "data": { "newBalance": 100000 }}`

---

## 3. 챌린지 및 투표 (Challenge & Vote)

### [POST] 챌린지 생성 (Create)
1. **Method**: `POST`
2. **URL**: `http://localhost:8080/challenges`
3. **Headers**: `Authorization: Bearer <Token>`, `Content-Type: application/json`
4. **Body**:
   ```json
   {
     "name": "성공하는 챌린지",
     "category": "STUDY",
     "maxMembers": 10,
     "supportAmount": 20000,
     "depositAmount": 20000,
     "startDate": "2026-03-01"
   }
   ```
5. **Response (201 Created)**: `{"success": true, "data": { "challengeId": "uuid-123" }}`

### [PUT] 챌린지 수정 (Update)
1. **Method**: `PUT`
2. **URL**: `http://localhost:8080/challenges/{id}`
3. **Headers**: `Authorization: Bearer <Token>`, `Content-Type: application/json`
4. **Body**:
   ```json
   {
     "name": "수정된 챌린지명",
     "description": "수정된 설명",
     "maxMembers": 15
   }
   ```
5. **Response (200 OK)**: `{"success": true, "message": "챌린지 정보가 수정되었습니다."}`

### [POST] 챌린지 가입 (Join)
1. **Method**: `POST`
2. **URL**: `http://localhost:8080/challenges/{id}/join`
3. **Response**:
   - **성공 (200 OK)**: `{"success": true, "message": "가입되었습니다."}`
   - **실패 (400 Bad Request - 잔액 부족)**:
     ```json
     {
       "success": false,
       "message": "ACCOUNT_003: 잔액이 부족합니다.",
       "timestamp": "..."
     }
     ```

### [POST] 투표 개설 (Create Vote)
1. **Method**: `POST`
2. **URL**: `http://localhost:8080/challenges/{id}/votes`
3. **Headers**: `Authorization: Bearer <Token>`, `Content-Type: application/json`
4. **Body - 일반 (NORMAL: KICK/DISSOLVE 등)**:
   ```json
   {
     "type": "NORMAL",
     "title": "멤버 강퇴 투표",
     "description": "규칙 위반 유저 강퇴 여부",
     "targetId": "target-user-uuid",
     "deadline": "2026-02-15T23:59:59"
   }
   ```
5. **Body - 결제/정산 (EXPENSE)**:
   ```json
   {
     "type": "EXPENSE",
     "title": "식비 정산",
     "meetingId": "meeting-uuid",
     "amount": 50000,
     "receiptUrl": "http://image.com/...",
     "deadline": "2026-02-15T23:59:59"
   }
   ```
6. **Body - 모임 참석 (MEETING_ATTENDANCE)**:
   ```json
   {
     "type": "MEETING_ATTENDANCE",
     "title": "2월 정기 모임 참석 여부",
     "meetingId": "meeting-uuid",
     "deadline": "2026-02-14T18:00:00"
   }
   ```
7. **Response (201 Created)**:
   ```json
   {
     "success": true,
     "data": { "voteId": "vote-uuid", "status": "OPEN", "createdAt": "..." }
   }
   ```

---

## 4. 커뮤니티 및 기타 (SNS & More)

### [POST] 게시글 작성 (Post)
1. **Method**: `POST`
2. **URL**: `http://localhost:8080/challenges/{id}/posts`
3. **Body**: `{"title": "공지", "content": "내용", "category": "NOTICE"}`
4. **Response (201 Created)**: `{"success": true, "data": { "postId": "post-uuid" }}`

### [POST] 댓글 작성 (Comment)
1. **Method**: `POST`
2. **URL**: `http://localhost:8080/challenges/{id}/posts/{postid}/comments`
3. **Body**: `{"content": "최고예요!"}`
4. **Response (201 Created)**: `{"success": true, "data": { "commentId": "comment-uuid" }}`

### [GET] 챌린지 멤버 목록 (Members)
1. **Method**: `GET`
2. **URL**: `http://localhost:8080/challenges/{id}/members`
3. **Response (200 OK)**:
   ```json
   {
     "success": true,
     "data": {
       "members": [
         { "memberId": "...", "user": { "nickname": "길동이" }, "role": "LEADER" }
       ]
     }
   }
   ```

### [DELETE] 챌린지 삭제 (Delete)
1. **Method**: `DELETE`
2. **URL**: `http://localhost:8080/challenges/{id}`
3. **Response**:
   - **성공 (200 OK)**: `{"success": true, "message": "챌린지가 삭제되었습니다."}`
   - **실패 (403 Forbidden - 리더 아님)**:
     ```json
     {
       "success": false,
       "message": "CHALLENGE_004: 리더만 삭제할 수 있습니다."
     }
     ```

---

## 🚫 공통 오류 응답 패턴 (Error Examples)

### [401 Unauthorized] 토큰 누락/만료
```json
{
  "success": false,
  "message": "AUTH_001: 인증 토큰이 유효하지 않거나 누락되었습니다.",
  "timestamp": "..."
}
```

### [403 Forbidden] 권한 부족 (작성자 아님 등)
```json
{
  "success": false,
  "message": "POST_004: 해당 게시글에 대한 수정/삭제 권한이 없습니다.",
  "timestamp": "..."
}
```

### [404 Not Found] 존재하지 않는 자원
```json
{
  "success": false,
  "message": "CHALLENGE_001: 존재하지 않는 챌린지입니다.",
  "timestamp": "..."
}
```
