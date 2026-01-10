#!/bin/bash
# 1. Generate keys
openssl genrsa -out private.pem 2048 2>/dev/null
openssl rsa -in private.pem -pubout -outform DER | base64 | tr -d '\n' > public.b64

# 2. Create payload
echo -n '{"expireAt":"2099-12-31","maxUsers":99999,"nodeLimit":99999}' > payload.json

# 3. Sign
openssl dgst -sha256 -sign private.pem -out signature.bin payload.json

# 4. Prepare JSON parts
PAYLOAD_B64=$(base64 < payload.json | tr -d '\n')
SIG_B64=$(base64 < signature.bin | tr -d '\n')

# 5. Output Public Key and License JSON
echo "---PUBLIC KEY---"
cat public.b64
echo ""
echo "---LICENSE JSON---"
echo "{\"payload\":\"$PAYLOAD_B64\",\"sig\":\"$SIG_B64\"}"
