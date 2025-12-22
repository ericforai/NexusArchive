# Input: Python
# Output: 运维脚本逻辑
# Pos: 脚本工具
# 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

import hmac
import hashlib
import json
import urllib.request
import sys

# Configuration
APP_SECRET = "e9a58fd35f3ca3f0a46d27b8859758b1ed35f0b6"
URL = "http://localhost:8080/api/integration/yonsuite/webhook"

# Payload
payload = {
    "data": {
        "id": "V-20251205-001",
        "orgId": "ORG-001"
    },
    "type": "voucher_audit"
}

# Convert payload to string (compact JSON)
body = json.dumps(payload, separators=(',', ':'))

# Calculate Signature
signature = hmac.new(
    APP_SECRET.encode('utf-8'),
    body.encode('utf-8'),
    hashlib.sha256
).hexdigest()

print(f"--- YonSuite Webhook Simulation ---")
print(f"Target URL: {URL}")
print(f"App Secret: {APP_SECRET}")
print(f"Body: {body}")
print(f"Calculated Signature: {signature}")
print(f"-----------------------------------")

# Execute Request
try:
    req = urllib.request.Request(URL, data=body.encode('utf-8'), method='POST')
    req.add_header('Content-Type', 'application/json')
    req.add_header('signature', signature)
    
    with urllib.request.urlopen(req) as response:
        print(f"Response Status: {response.status}")
        print(f"Response Body: {response.read().decode('utf-8')}")
except urllib.error.URLError as e:
    print(f"Error sending request: {e}")
    print("\n[!] Connection Failed. Is the backend server running on localhost:8080?")
except Exception as e:
    print(f"An error occurred: {e}")
