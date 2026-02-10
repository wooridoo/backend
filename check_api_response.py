import requests
import json

BASE_URL = "http://localhost:8080"
# Use the token from the previous successful run if possible, or login again.
# I'll just login as the user created in previous test if I know the creds, or create a new one.
# Actually I'll create a new one to be safe.
import uuid

email = f"check_{uuid.uuid4().hex[:6]}@test.com"
password = "Password123!"
nickname = f"Check_{uuid.uuid4().hex[:6]}"

try:
    # Signup
    resp = requests.post(f"{BASE_URL}/auth/signup", json={
        "email": email,
        "password": password,
        "nickname": nickname,
        "name": "Checker",
        "phone": "010-0000-0000",
        "birthDate": "2000-01-01",
        "gender": "M",
        "termsAgreed": True,
        "privacyAgreed": True
    })
    
    # Login
    resp = requests.post(f"{BASE_URL}/auth/login", json={"email": email, "password": password})
    token = resp.json()["data"]["accessToken"]
    headers = {"Authorization": f"Bearer {token}"}
    
    # Get Me
    print("\n--- GET /users/me ---")
    resp = requests.get(f"{BASE_URL}/users/me", headers=headers)
    print(json.dumps(resp.json(), indent=2, ensure_ascii=False))

    # Get Account
    print("\n--- GET /accounts/me ---")
    resp = requests.get(f"{BASE_URL}/accounts/me", headers=headers)
    print(json.dumps(resp.json(), indent=2, ensure_ascii=False))

except Exception as e:
    print(e)
