
import os
import sys
import requests
import json

BASE_URL = os.environ.get("BASE_URL", "http://localhost:19090/api")
USERNAME = "admin"
PASSWORD = "admin123"

def main():
    session = requests.Session()
    # Login
    resp = session.post(f"{BASE_URL}/auth/login", json={"username": USERNAME, "password": PASSWORD})
    if resp.status_code != 200:
        print("Login failed")
        return
    token = resp.json()["data"]["token"]
    session.headers["Authorization"] = f"Bearer {token}"

    print("\n--- Testing AC03 + READY_TO_ARCHIVE (Default Filter) ---")
    # Legacy status for READY_TO_ARCHIVE is PENDING_ARCHIVE
    url = f"{BASE_URL}/pool/list/status/PENDING_ARCHIVE?category=AC03"
    print(f"GET {url}")
    resp = session.get(url)
    if resp.status_code == 200:
        data = resp.json()["data"]
        print(f"Response Count: {len(data)}")
    else:
        print(f"Error: {resp.text}")

    print("\n--- Testing AC03 + PENDING_CHECK (Actual Status) ---")
    url = f"{BASE_URL}/pool/list/status/PENDING_CHECK?category=AC03"
    print(f"GET {url}")
    resp = session.get(url)
    if resp.status_code == 200:
        data = resp.json()["data"]
        print(f"Response Count: {len(data)}")
    else:
        print(f"Error: {resp.text}")
    print(f"GET {url}")
    resp = session.get(url)
    if resp.status_code == 200:
        data = resp.json()["data"]
        print(f"Response Count: {len(data)}")
    else:
        print(f"Error: {resp.text}")

if __name__ == "__main__":
    main()
