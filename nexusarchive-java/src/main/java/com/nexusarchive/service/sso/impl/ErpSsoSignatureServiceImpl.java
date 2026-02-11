// Input: Java Crypto、Spring
// Output: ErpSsoSignatureServiceImpl
// Pos: SSO 服务层实现

package com.nexusarchive.service.sso.impl;

import com.nexusarchive.exception.ErpSsoException;
import com.nexusarchive.service.sso.ErpSsoSignatureService;
import com.nexusarchive.service.sso.SsoErrorCodes;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Base64;

@Service
public class ErpSsoSignatureServiceImpl implements ErpSsoSignatureService {

    private static final String HMAC_SHA_256 = "HmacSHA256";

    @Override
    public String sign(String payload, String secret) {
        try {
            Mac mac = Mac.getInstance(HMAC_SHA_256);
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), HMAC_SHA_256));
            byte[] signed = mac.doFinal(payload.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(signed);
        } catch (Exception e) {
            throw new ErpSsoException(SsoErrorCodes.INVALID_SIGNATURE, "签名计算失败", 401);
        }
    }

    @Override
    public boolean verify(String payload, String secret, String signature) {
        String expected = sign(payload, secret);
        return MessageDigest.isEqual(expected.getBytes(StandardCharsets.UTF_8),
                signature.getBytes(StandardCharsets.UTF_8));
    }

    @Override
    public void validateTimestamp(Long timestampSeconds, long nowSeconds, long allowedSkewSeconds) {
        if (timestampSeconds == null) {
            throw new ErpSsoException(SsoErrorCodes.TIMESTAMP_EXPIRED, "缺少时间戳", 401);
        }
        long delta = Math.abs(nowSeconds - timestampSeconds);
        if (delta > allowedSkewSeconds) {
            throw new ErpSsoException(SsoErrorCodes.TIMESTAMP_EXPIRED, "请求时间戳已过期", 401);
        }
    }
}
