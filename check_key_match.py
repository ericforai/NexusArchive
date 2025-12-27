#!/usr/bin/env python3
# -*- coding: utf-8 -*-

# Input: PEM key files
# Output: Key match verification results
# Pos: DevOps/Scripts - Security Utility
# 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

import base64
from cryptography.hazmat.primitives import serialization
from cryptography.hazmat.backends import default_backend

# Configured Public Key from application.yml
PUB_KEY_B64 = "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAxMrI4RYBcukhqboFUKZlIMIANgPyt5HK6m4DK6V5bt6CVSPD2V1vKBQ3dpYo4vBZKAk6zOv3Uyq2fEPijKFozQwBo/YNu05LV6bJHu696lPaevK9Y04Grrkj2oOIH8aBqMNfotrBAPFso2IbVlnoEiS6K8AYgQ1GrfvIYoEpfeqDF3rhvwMgauWVez3nxHbGUvnvRxik77PNoBnnuO7YpgQTK6XxdxtsV1EYn0/GSL1EdwadgdxwJHwoQL4vI07lNkbI/dBiF9RRamTX8u+7kFDuw0J0e6nlD9C9vcMX/e6HnCETK9AIb1axqi1cjE1A+5xJ07ONoVYo4xTPbFVCOQIDAQAB"

CANDIDATES = [
    "/Users/user/nexusarchive/nexusarchive-java/keystore/private_key.pem",
    "/Users/user/nexusarchive/nexusarchive-java/keystore/jwt_private.pem",
    "/Users/user/nexusarchive/nexusarchive-java/src/main/resources/certs/private.pem"
]

def load_public_key_from_b64(b64_str):
    der_data = base64.b64decode(b64_str)
    return serialization.load_der_public_key(der_data, backend=default_backend())

def check_match(priv_path, target_pub_key):
    try:
        with open(priv_path, "rb") as f:
            pem_data = f.read()
            # Try loading as PKCS8
            try:
                priv_key = serialization.load_pem_private_key(pem_data, password=None, backend=default_backend())
            except:
                # Try loading without password (if encrypted, will fail)
                return False

            derived_pub = priv_key.public_key()
            
            # Compare public numbers
            target_nums = target_pub_key.public_numbers()
            derived_nums = derived_pub.public_numbers()
            
            if target_nums == derived_nums:
                return True
    except Exception as e:
        print(f"Error checking {priv_path}: {e}")
    return False

target_pub = load_public_key_from_b64(PUB_KEY_B64)

for path in CANDIDATES:
    if check_match(path, target_pub):
        print(f"MATCH: {path}")
        exit(0)

print("NO MATCH FOUND")
exit(1)
