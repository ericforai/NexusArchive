
#!/bin/bash

# Configuration
API_URL="http://localhost:19090/api/v1/archive/sip/ingest"
REQUEST_ID="REQ-ABNORMAL-TEST-$(date +%s)"

# Create a JSON payload with intentionally invalid data to trigger compliance failure
# Example: Missing mandatory fields or logic errors (like unbalanced debit/credit if checked)
# Here we'll try sending a payload that might fail "Integrity" or "Authenticity" checks if implemented
# Or simply use a valid structure but invalid content

cat > abnormal_payload.json <<EOF
{
  "requestId": "$REQUEST_ID",
  "sourceSystem": "TEST_ERP",
  "header": {
    "fondsCode": "FONDS-001",
    "voucherNumber": "V-INVALID-001",
    "accountPeriod": "2025-01",
    "voucherDate": "2025-01-15",
    "voucherType": "记账凭证",
    "attachmentCount": 0,
    "preparedBy": "TestUser",
    "issuer": "TestUser"
  },
  "entries": [],
  "files": []
}
EOF

# Send request
echo "Sending SIP with Request ID: $REQUEST_ID"
curl -X POST "$API_URL" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer mock-token" \
  -d @abnormal_payload.json

echo -e "\n\nDone. Check the Abnormal Isolation Area specific page."
rm abnormal_payload.json
