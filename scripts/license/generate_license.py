#!/usr/bin/env python3
# -*- coding: utf-8 -*-

# Input: Crypto libraries, License templates
# Output: Signed license.json
# Pos: DevOps/Scripts - License Generation
# 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

import base64
import json
import datetime
from cryptography.hazmat.primitives import serialization, hashes
from cryptography.hazmat.primitives.asymmetric import padding
from cryptography.hazmat.backends import default_backend

# 1. Generate Key Pair
private_key = default_backend().generate_rsa_private_key(
    public_exponent=65537,
    key_size=2048
)
public_key = private_key.public_key()

# 2. Export Properties
# Public Key in DER -> Base64 (for application.yml/env)
pub_der = public_key.public_bytes(
    encoding=serialization.Encoding.DER,
    format=serialization.PublicFormat.SubjectPublicKeyInfo
)
pub_b64 = base64.b64encode(pub_der).decode('utf-8')

print(f"NEW_PUBLIC_KEY_B64:\n{pub_b64}\n")

# 3. Create License Payload (Permanent)
payload_data = {
    "expireAt": "2099-12-31",
    "maxUsers": 99999,
    "nodeLimit": 99999
}
payload_json = json.dumps(payload_data)
payload_b64 = base64.b64encode(payload_json.encode('utf-8')).decode('utf-8')

# 4. Sign Payload (SHA256withRSA)
signature = private_key.sign(
    payload_json.encode('utf-8'),
    padding.PKCS1v15(),
    hashes.SHA256()
)
sig_b64 = base64.b64encode(signature).decode('utf-8')

# 5. Final License JSON
license_wrapper = {
    "payload": payload_b64,
    "sig": sig_b64
}
license_json = json.dumps(license_wrapper)

print(f"LICENSE_JSON:\n{license_json}\n")

# Save to files for reference
with open("new_license_pub.txt", "w") as f:
    f.write(pub_b64)
with open("new_license.json", "w") as f:
    f.write(license_json)
