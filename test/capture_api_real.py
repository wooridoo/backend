import requests
import json
import uuid
import time
from datetime import datetime, timedelta

BASE_URL = "http://localhost:8080"
TIMEOUT = 10

def log_api(method, path, headers=None, body=None, description=""):
    url = f"{BASE_URL}{path}"
    print(f"\n### {description}")
    print(f"- **Method**: {method}")
    print(f"- **URL**: {url}")
    
    if headers:
        print("- **Request Headers**:")
        print("  ```http")
        for k, v in headers.items():
            if k == "Authorization" and len(v) > 50:
                print(f"  {k}: {v[:30]}... (truncated)")
            else:
                print(f"  {k}: {v}")
        print("  ```")
    
    if body:
        print("- **Request Body**:")
        print("  ```json")
        print(json.dumps(body, indent=2, ensure_ascii=False))
        print("  ```")
    
    try:
        resp = None
        if method == "GET":
            resp = requests.get(url, headers=headers, timeout=TIMEOUT)
        elif method == "POST":
            resp = requests.post(url, headers=headers, json=body, timeout=TIMEOUT)
        elif method == "PUT":
            resp = requests.put(url, headers=headers, json=body, timeout=TIMEOUT)
        elif method == "DELETE":
            resp = requests.delete(url, headers=headers, json=body, timeout=TIMEOUT)
        
        print(f"- **Response Status**: {resp.status_code}")
        print("- **Response Body**:")
        print("  ```json")
        try:
            print(json.dumps(resp.json(), indent=2, ensure_ascii=False))
        except:
            print(resp.text)
        print("  ```")
        return resp
    except Exception as e:
        print(f"Error: {e}")
        return None

def main():
    # 1. Auth & Signup
    email_a = f"real_{uuid.uuid4().hex[:4]}@test.com"
    pass_a = "password123!"
    nick_a = f"User_{uuid.uuid4().hex[:4]}"
    
    log_api("POST", "/auth/signup", body={
        "email": email_a, "password": pass_a, "nickname": nick_a, "name": "A",
        "phone": "010-0000-0000", "birthDate": "1990-01-01", "gender": "F",
        "verificationToken": "TEST", "termsAgreed": True, "privacyAgreed": True
    }, description="[Auth] signup")

    res = requests.post(f"{BASE_URL}/auth/login", json={"email": email_a, "password": pass_a})
    token_a = res.json()["data"]["accessToken"]
    refresh_token = res.json()["data"]["refreshToken"]
    headers_a = {"Authorization": f"Bearer {token_a}", "Content-Type": "application/json"}

    log_api("POST", "/auth/refresh", body={"refreshToken": refresh_token}, description="[Auth] refresh")
    log_api("POST", "/auth/password/reset", body={"email": email_a}, description="[Auth] password reset request")

    # 2. User
    log_api("PUT", "/users/me", headers=headers_a, body={"nickname": nick_a + "X"}, description="[User] update profile")

    # 3. Account
    log_api("POST", "/accounts/charge", headers=headers_a, body={
        "amount": 200000, "paymentMethod": "CARD", "returnUrl": "http://localhost:3000"
    }, description="[Account] charge request")

    # 4. Challenge
    chall_res = log_api("POST", "/challenges", headers=headers_a, body={
        "name": "REAL CHALLENGE", "category": "SAVINGS", "maxMembers": 5,
        "supportAmount": 20000, "depositAmount": 20000,
        "startDate": (datetime.now() + timedelta(days=10)).strftime("%Y-%m-%d")
    }, description="[Challenge] create")
    
    if chall_res and chall_res.status_code == 201:
        challenge_id = chall_res.json()["data"]["challengeId"]
        log_api("GET", f"/challenges/{challenge_id}", headers=headers_a, description="[Challenge] detail")
        log_api("GET", f"/challenges/{challenge_id}/account", headers=headers_a, description="[Challenge] account")
        
        # 5. SNS
        post_res = log_api("POST", f"/challenges/{challenge_id}/posts", headers=headers_a, body={
            "title": "NOTICE", "content": "IMPORTANT", "category": "NOTICE"
        }, description="[SNS] create notice")
        
        if post_res and post_res.status_code == 201:
            post_id = post_res.json()["data"]["postId"]
            log_api("POST", f"/challenges/{challenge_id}/posts/{post_id}/comments", headers=headers_a, body={
                "content": "REAL COMMENT"
            }, description="[SNS] create comment")

if __name__ == "__main__":
    main()
