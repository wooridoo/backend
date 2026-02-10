import requests
import json
import uuid
import time
from datetime import datetime, timedelta

BASE_URL = "http://localhost:8080"
TIMEOUT = 45

def log_call(headers, method, path, body=None, description=""):
    url = f"{BASE_URL}{path}"
    print(f"\n## {description}")
    print(f"1. **Method**: {method}")
    print(f"2. **URL**: {url}")
    print(f"3. **Headers**: \n   ```http\n   Content-Type: application/json\n   Authorization: {headers.get('Authorization', 'None')[:30]}... (Live Token)\n   ```")
    if body:
        print(f"4. **Body**:\n   ```json\n{json.dumps(body, indent=2, ensure_ascii=False)}\n   ```")
    else:
        print(f"4. **Body**: None")
    
    try:
        if method == "POST": resp = requests.post(url, headers=headers, json=body, timeout=TIMEOUT)
        elif method == "GET": resp = requests.get(url, headers=headers, timeout=TIMEOUT)
        elif method == "PUT": resp = requests.put(url, headers=headers, json=body, timeout=TIMEOUT)
        elif method == "DELETE": resp = requests.delete(url, headers=headers, json=body, timeout=TIMEOUT)
        
        print(f"5. **Response** (Status: {resp.status_code}):\n   ```json\n{json.dumps(resp.json(), indent=2, ensure_ascii=False)}\n   ```")
        return resp
    except Exception as e:
        print(f"5. **Response** (Error): {e}")
        return None

def main():
    # 1. Quick Auth for Leader & Member
    email_l = f"l_{uuid.uuid4().hex[:4]}@api.com"
    email_m = f"m_{uuid.uuid4().hex[:4]}@api.com"
    nick_l = f"L_{uuid.uuid4().hex[:4]}"
    nick_m = f"M_{uuid.uuid4().hex[:4]}"
    passw = "pass123!"

    requests.post(f"{BASE_URL}/auth/signup", json={"email": email_l, "password": passw, "nickname": nick_l, "name": "L", "phone": "010-0000-0000", "birthDate": "1990-01-01", "gender": "M", "verificationToken": "TEST", "termsAgreed": True, "privacyAgreed": True})
    requests.post(f"{BASE_URL}/auth/signup", json={"email": email_m, "password": passw, "nickname": nick_m, "name": "M", "phone": "010-0000-0000", "birthDate": "1990-01-01", "gender": "M", "verificationToken": "TEST", "termsAgreed": True, "privacyAgreed": True})
    
    token_l = requests.post(f"{BASE_URL}/auth/login", json={"email": email_l, "password": passw}).json()["data"]["accessToken"]
    token_m = requests.post(f"{BASE_URL}/auth/login", json={"email": email_m, "password": passw}).json()["data"]["accessToken"]
    
    headers_l = {"Content-Type": "application/json", "Authorization": f"Bearer {token_l}"}
    headers_m = {"Content-Type": "application/json", "Authorization": f"Bearer {token_m}"}

    # 2. Challenge Join
    res_ch = requests.post(f"{BASE_URL}/challenges", headers=headers_l, json={"name": "JOIN TEST", "description": "X", "category": "FOOD", "maxMembers": 10, "supportAmount": 10000, "depositAmount": 10000, "startDate": "2026-03-01"})
    chall_id = res_ch.json()["data"]["challengeId"]
    
    # Member needs money
    res_c = requests.post(f"{BASE_URL}/accounts/charge", headers=headers_m, json={"amount": 10000, "paymentMethod": "CARD", "returnUrl": "X"})
    requests.post(f"{BASE_URL}/accounts/charge/callback", headers=headers_m, json={"orderId": res_c.json()["data"]["orderId"], "paymentKey": "K", "amount": 10000, "status": "SUCCESS"})
    
    log_call(headers_m, "POST", f"/challenges/{chall_id}/join", description="챌린지 가입")

    # 3. Vote Create
    log_call(headers_l, "POST", f"/challenges/{chall_id}/votes", body={"type": "NORMAL", "title": "메뉴 선정", "deadline": "2026-02-15T23:00:00"}, description="투표 생성")

    # 4. User Withdrawal
    log_call(headers_m, "DELETE", "/users/me", body={"password": passw, "reason": "TEST END"}, description="회원 탈퇴")

if __name__ == "__main__":
    main()
