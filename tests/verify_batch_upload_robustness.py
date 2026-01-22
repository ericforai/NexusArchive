
import os
import sys
import requests
import json
import time
import subprocess

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

    def create_batch(self, payload):
        print(f"Creating batch with payload: {payload}")
        headers = {"X-Fonds-No": payload.get("fondsCode")}
        return self.session.post(f"{self.base_url}/collection/batch/create", json=payload, headers=headers).json()

def run_db_query(sql):
    cmd = f'docker exec nexus-db psql -U postgres -d nexusarchive -t -c "{sql}"'
    result = subprocess.run(cmd, shell=True, capture_output=True, text=True)
    return result.stdout.strip()

def main():
    client = APIClient(BASE_URL)
    if not client.login():
        sys.exit(1)

    print("\n--- Shadow Inspector: Batch Upload Verification ---")

    # 1. API Action: Create Batch (using previously failing category AC03)
    payload = {
        "batchName": "ShadowTest-Batch",
        "fondsCode": "DEMO",
        "fiscalYear": "2024",
        "archivalCategory": "AC03",
        "totalFiles": 1,
        "autoCheck": True
    }
    
    resp = client.create_batch(payload)
    
    if resp.get("code") != 200:
        print(f"❌ API Action Failed: {resp}")
        sys.exit(1)
    
    batch_data = resp.get("data", {})
    batch_id = batch_data.get("batchId")
    batch_no = batch_data.get("batchNo")
    print(f"✅ API Action Success: Created Batch ID {batch_id}, No {batch_no}")

    # 2. DB Verification (Layer 2)
    print("\n--- Layer 2: DB Verification ---")
    
    # Verify Batch Record
    db_category = run_db_query(f"SELECT archival_category FROM collection_batch WHERE id = {batch_id}")
    db_fonds = run_db_query(f"SELECT fonds_code FROM collection_batch WHERE id = {batch_id}")
    
    print(f"DB Check: Category='{db_category}', Fonds='{db_fonds}'")
    
    if db_category.strip() == "AC03" and db_fonds.strip() == "DEMO":
        print("✅ DB Verification Passed: Record matches logic")
    else:
        print(f"❌ DB Verification Failed: Expected AC03/DEMO, got '{db_category}'/'{db_fonds}'")
        sys.exit(1)

    # 3. Audit Verification (Layer 2)
    print("\n--- Layer 2: Audit Verification ---")
    # Check for recent audit log
    sql = f"SELECT COUNT(*) FROM sys_audit_log WHERE action = 'CREATE_BATCH' AND created_at > NOW() - INTERVAL '5 minutes'"
    audit_count_str = run_db_query(sql).strip()
    
    # Handle empty string or default to 0
    audit_count = int(audit_count_str) if audit_count_str.isdigit() else 0
    
    if audit_count > 0:
        print(f"✅ Audit Verification Passed: Found {audit_count} log entries")
    else:
        print("❌ Audit Verification Failed: No audit log found (Count=0)")
        sys.exit(1)

    print("\n🎉 All Shadow Invariants Passed!")

if __name__ == "__main__":
    main()
