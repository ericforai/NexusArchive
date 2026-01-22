
import os
import sys
import requests
import json
import time

# Configuration
BASE_URL = os.environ.get("BASE_URL", "http://localhost:19090/api")
USERNAME = "admin"
PASSWORD = "admin123"

class APIClient:
    def __init__(self, base_url):
        self.base_url = base_url.rstrip("/")
        self.session = requests.Session()
        self.token = None

    def login(self):
        print(f"Logging in to {self.base_url}...")
        try:
            resp = self.session.post(
                f"{self.base_url}/auth/login",
                json={"username": USERNAME, "password": PASSWORD},
                timeout=5
            )
            data = resp.json()
            if data.get("code") == 200:
                self.token = data["data"]["token"]
                self.session.headers["Authorization"] = f"Bearer {self.token}"
                print("Login successful.")
                return True
            print(f"Login failed: {data}")
            return False
        except Exception as e:
            print(f"Login error: {e}")
            return False

    def get_pool_list(self, category=None):
        url = f"{self.base_url}/pool/list/status/PENDING_CHECK"
        params = {}
        if category:
            params["category"] = category
        
        resp = self.session.get(url, params=params)
        return resp.json()

def main():
    client = APIClient(BASE_URL)
    if not client.login():
        sys.exit(1)

    # Categories to test
    # Mapping: Category -> Expected voucher_type(s)
    test_cases = [
        ("VOUCHER", ["VOUCHER"]),
        ("AC01", ["AC01", "ATTACHMENT"]),
        ("AC02", ["AC02", "LEDGER"]),
        ("AC03", ["AC03", "REPORT"]),
        ("AC04", ["AC04", "OTHER", None]) # None is tricky to assert from JSON, usually missing or null
    ]

    failure = False

    print("\n--- Verifying Category Filtering ---")

    for category, expected_types in test_cases:
        print(f"\nTesting Category: {category}")
        print(f"Expected Voucher Types: {expected_types}")
        
        resp = client.get_pool_list(category)
        if resp.get("code") != 200:
            print(f"FAILED: API Error {resp.get('code')} - {resp.get('message')}")
            failure = True
            continue

        items = resp.get("data", [])
        print(f"Items returned: {len(items)}")

        for item in items:
            v_type = item.get("voucherType")
            print(f"  - Item ID: {item.get('id')}, Type: {v_type}")
            
            # Special handling for NULL/None
            if v_type is None and None in expected_types:
                continue

            if v_type not in expected_types:
                print(f"    ERROR: Found type '{v_type}' but expected {expected_types}")
                failure = True
    
    if failure:
        print("\n❌ Verification FAILED")
        sys.exit(1)
    else:
        print("\n✅ Verification PASSED")
        sys.exit(0)

if __name__ == "__main__":
    main()
