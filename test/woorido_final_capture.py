import requests
import json
import uuid
import time
from datetime import datetime, timedelta

BASE_URL = "http://localhost:8080"
TIMEOUT = 60 # Maximum patience

def log(desc, method, url, headers, body, resp):
    print(f"\n## {desc}")
    print(f"1. **Method**: {method}")
    print(f"2. **URL**: {url}")
    print(f"3. **Headers**: \n   ```http\n   Content-Type: application/json\n   Authorization: {headers.get('Authorization', 'None')[:40]}... (Live Token)\n   ```")
    if body:
        print(f"4. **Body**:\n   ```json\n{json.dumps(body, indent=2, ensure_ascii=False)}\n   ```")
    else:
        print(f"4. **Body**: None")
    
    if resp:
        print(f"5. **Response** (Status: {resp.status_code}):\n   ```json\n{json.dumps(resp.json(), indent=2, ensure_ascii=False)}\n   ```")
    else:
        print(f"5. **Response**: TIMEOUT or ERROR")

def main():
    u = f"u_{uuid.uuid4().hex[:4]}@final.com"
    p = "pass123!"
    n = f"N_{uuid.uuid4().hex[:4]}"
    
    # Setup
    requests.post(f"{BASE_URL}/auth/signup", json={"email": u, "password": p, "nickname": n, "name": "U", "phone": "010-0000-0000", "birthDate": "1990-01-01", "gender": "M", "verificationToken": "TEST", "termsAgreed": True, "privacyAgreed": True})
    token = requests.post(f"{BASE_URL}/auth/login", json={"email": u, "password": p}).json()["data"]["accessToken"]
    h = {"Content-Type": "application/json", "Authorization": f"Bearer {token}"}
    
    # 1. Charge for Join
    c_res = requests.post(f"{BASE_URL}/accounts/charge", headers=h, json={"amount": 50000, "paymentMethod": "CARD", "returnUrl": "X"}).json()
    requests.post(f"{BASE_URL}/accounts/charge/callback", headers=h, json={"orderId": c_res["data"]["orderId"], "paymentKey": "K", "amount": 50000, "status": "SUCCESS"})

    # 2. Challenge Join
    # Need a challenge first.
    ch_res = requests.post(f"{BASE_URL}/challenges", headers=h, json={"name": "FINAL CAPTURE", "category": "STUDY", "maxMembers": 5, "supportAmount": 10000, "depositAmount": 10000, "startDate": "2026-03-01"})
    cid = ch_res.json()["data"]["challengeId"]
    
    # Actually, as a leader I'm already in. Let's create another user to join.
    u2 = f"u2_{uuid.uuid4().hex[:4]}@final.com"
    requests.post(f"{BASE_URL}/auth/signup", json={"email": u2, "password": p, "nickname": n+"2", "name": "U2", "phone": "010-0000-0000", "birthDate": "1990-01-01", "gender": "F", "verificationToken": "TEST", "termsAgreed": True, "privacyAgreed": True})
    token2 = requests.post(f"{BASE_URL}/auth/login", json={"email": u2, "password": p}).json()["data"]["accessToken"]
    h2 = {"Content-Type": "application/json", "Authorization": f"Bearer {token2}"}
    c_res2 = requests.post(f"{BASE_URL}/accounts/charge", headers=h2, json={"amount": 50000, "paymentMethod": "CARD", "returnUrl": "X"}).json()
    requests.post(f"{BASE_URL}/accounts/charge/callback", headers=h2, json={"orderId": c_res2["data"]["orderId"], "paymentKey": "K2", "amount": 50000, "status": "SUCCESS"})
    
    url_join = f"{BASE_URL}/challenges/{cid}/join"
    try:
        r_join = requests.post(url_join, headers=h2, timeout=TIMEOUT)
        log("챌린지 가입", "POST", url_join, h2, None, r_join)
    except: log("챌린지 가입", "POST", url_join, h2, None, None)

    # 3. Vote Types
    for vtype in ["NORMAL", "ATTENDANCE", "SETTLEMENT"]:
        url_v = f"{BASE_URL}/challenges/{cid}/votes"
        body_v = {"type": vtype, "title": f"{vtype} 투표", "description": "설명", "deadline": (datetime.now()+timedelta(days=1)).strftime("%Y-%m-%dT23:59:59")}
        try:
            rv = requests.post(url_v, headers=h, json=body_v, timeout=TIMEOUT)
            log(f"투표 생성 ({vtype})", "POST", url_v, h, body_v, rv)
        except: log(f"투표 생성 ({vtype})", "POST", url_v, h, body_v, None)

    # 4. User Withdrawal
    url_w = f"{BASE_URL}/users/me"
    body_w = {"password": p, "reason": "FINAL"}
    try:
        rw = requests.delete(url_w, headers=h2, json=body_w, timeout=TIMEOUT)
        log("회원 탈퇴", "DELETE", url_w, h2, body_w, rw)
    except: log("회원 탈퇴", "DELETE", url_w, h2, body_w, None)

if __name__ == "__main__":
    main()
