import requests
import json
import uuid
import time
from datetime import datetime, timedelta

# ==========================================
# CONFIGURATION
# ==========================================
BASE_URL = "http://localhost:8080"
TIMEOUT = 45 # Increased timeout
SLEEP_BETWEEN = 1.0 # Significant delay to prevent server overload

class WooridoDocumentationTester:
    def __init__(self, name):
        self.name = name
        self.email = f"{name.lower()}_{uuid.uuid4().hex[:4]}@api.com"
        self.password = "pass123!"
        self.nickname = f"Nick_{name}_{uuid.uuid4().hex[:4]}"
        self.headers = {"Content-Type": "application/json"}
        self.user_id = None
        self.tokens = {}

    def call(self, method, path, body=None, description=""):
        url = f"{BASE_URL}{path}"
        
        print(f"\n## {description}")
        print(f"1. **Method**: {method}")
        print(f"2. **URL**: {url}")
        print(f"3. **Headers**:")
        print("   ```http")
        for k, v in self.headers.items():
            if k == "Authorization" and len(v) > 50:
                print(f"   {k}: {v[:30]}... (Live Token)")
            else:
                print(f"   {k}: {v}")
        print("   ```")
        
        if body:
            print(f"4. **Body**:")
            print("   ```json")
            print(json.dumps(body, indent=2, ensure_ascii=False))
            print("   ```")
        else:
            print(f"4. **Body**: None")
        
        try:
            time.sleep(SLEEP_BETWEEN)
            resp = None
            if method == "GET":
                resp = requests.get(url, headers=self.headers, timeout=TIMEOUT)
            elif method == "POST":
                resp = requests.post(url, headers=self.headers, json=body, timeout=TIMEOUT)
            elif method == "PUT":
                resp = requests.put(url, headers=self.headers, json=body, timeout=TIMEOUT)
            elif method == "DELETE":
                resp = requests.delete(url, headers=self.headers, json=body, timeout=TIMEOUT)
            
            if resp:
                print(f"5. **Response** (Status: {resp.status_code}):")
                print("   ```json")
                try:
                    print(json.dumps(resp.json(), indent=2, ensure_ascii=False))
                except:
                    print(resp.text)
                print("   ```")
            return resp
        except Exception as e:
            print(f"5. **Response** (Error): {e}")
            return None

def main():
    leader = WooridoDocumentationTester("Leader")
    member = WooridoDocumentationTester("Member")

    print("# Woorido Live API Execution Report")
    print("\n이 문서는 실제 서버 응답을 캡처한 데이터입니다.")

    # --- 1. AUTH ---
    leader.call("POST", "/auth/signup", body={
        "email": leader.email, "password": leader.password, "nickname": leader.nickname,
        "name": "Leader", "phone": "010-1111-1111", "birthDate": "1990-01-01", "gender": "M",
        "verificationToken": "TEST", "termsAgreed": True, "privacyAgreed": True
    }, description="회원가입 - 성공 (Leader)")

    leader.call("POST", "/auth/signup", body={
        "email": leader.email, "password": "any", "nickname": "any"
    }, description="회원가입 - 실패 (이메일 중복 USER_002)")

    res_l = leader.call("POST", "/auth/login", body={"email": leader.email, "password": leader.password}, description="로그인 - 성공")
    if res_l and res_l.status_code == 200:
        leader.tokens = res_l.json()["data"]
        leader.headers["Authorization"] = f"Bearer {leader.tokens['accessToken']}"
        leader.user_id = leader.tokens["user"]["userId"]

    member.call("POST", "/auth/signup", body={
        "email": member.email, "password": member.password, "nickname": member.nickname,
        "name": "Member", "phone": "010-2222-2222", "birthDate": "1992-02-02", "gender": "F",
        "verificationToken": "TEST", "termsAgreed": True, "privacyAgreed": True
    }, description="회원가입 - 성공 (Member)")
    res_m = member.call("POST", "/auth/login", body={"email": member.email, "password": member.password}, description="로그인 - 성공 (Member)")
    if res_m and res_m.status_code == 200:
        member.tokens = res_m.json()["data"]
        member.headers["Authorization"] = f"Bearer {member.tokens['accessToken']}"
        member.user_id = member.tokens["user"]["userId"]

    # --- 2. USER ---
    leader.call("GET", "/users/me", description="내 정보 조회")
    leader.call("PUT", "/users/me", body={"nickname": leader.nickname + "X", "phone": "010-9999-9999"}, description="내 정보 수정")
    leader.call("GET", f"/users/check-nickname?nickname={member.nickname}", description="닉네임 중복 체크 (불가)")

    # --- 3. ACCOUNT ---
    res_c = leader.call("POST", "/accounts/charge", body={"amount": 100000, "paymentMethod": "CARD", "returnUrl": "http://localhost:3000"}, description="잔액 충전 요청")
    if res_c and res_c.status_code == 200:
        order_id = res_c.json()["data"]["orderId"]
        leader.call("POST", "/accounts/charge/callback", body={"orderId": order_id, "paymentKey": "TEST_KEY", "amount": 100000, "status": "SUCCESS"}, description="충전 확인 (콜백)")

    # --- 4. CHALLENGE ---
    res_ch = leader.call("POST", "/challenges", body={
        "name": "최종 테스트 챌린지", "description": "REAL API CAPTURE", "category": "STUDY",
        "maxMembers": 10, "supportAmount": 20000, "depositAmount": 20000,
        "startDate": (datetime.now() + timedelta(days=15)).strftime("%Y-%m-%d")
    }, description="챌린지 생성")
    
    if res_ch and res_ch.status_code == 201:
        chall_id = res_ch.json()["data"]["challengeId"]
        
        # Member charge and join
        res_cm = member.call("POST", "/accounts/charge", body={"amount": 100000, "paymentMethod": "CARD", "returnUrl": "X"})
        if res_cm and res_cm.status_code == 200:
            member.call("POST", "/accounts/charge/callback", body={"orderId": res_cm.json()["data"]["orderId"], "paymentKey": "MK", "amount": 100000, "status": "SUCCESS"})
        
        member.call("POST", f"/challenges/{chall_id}/join", description="챌린지 가입")
        leader.call("GET", f"/challenges/{chall_id}", description="챌린지 상세")
        
        # --- 5. SNS ---
        res_p = leader.call("POST", f"/challenges/{chall_id}/posts", body={"title": "공지", "content": "내용", "category": "NOTICE"}, description="게시글 작성")
        if res_p and res_p.status_code == 201:
            post_id = res_p.json()["data"]["postId"]
            member.call("POST", f"/challenges/{chall_id}/posts/{post_id}/comments", body={"content": "댓글"}, description="댓글 작성")
            member.call("POST", f"/challenges/{chall_id}/posts/{post_id}/like", description="게시글 좋아요")

        # --- 6. VOTE ---
        res_v = leader.call("POST", f"/challenges/{chall_id}/votes", body={
            "type": "NORMAL", "title": "교재 선정", "deadline": (datetime.now() + timedelta(days=5)).strftime("%Y-%m-%dT23:00:00")
        }, description="투표 생성")
        if res_v and res_v.status_code == 201:
            v_data = res_v.json()["data"]
            v_id = v_data.get("id") or v_data.get("voteId")
            if v_id: member.call("PUT", f"/votes/{v_id}/cast", body={"choice": "1"}, description="투표하기")

    # --- 7. WITHDRAWAL ---
    member.call("DELETE", "/users/me", body={"password": member.password, "reason": "BYE"}, description="회원 탈퇴")

if __name__ == "__main__":
    main()
