package com.nexusarchive.integration.yonsuite.security;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.Duration;

/**
 * YonSuite Webhook 签名验证器
 * 验证规则: HMAC-SHA256(timestamp + "\n" + nonce + "\n" + body, appSecret)
 */
@Component
@Slf4j
public class YonSuiteSignatureValidator {

    @Value("${yonsuite.app-secret}")
    private String appSecret;

    private static final Duration ALLOWED_DRIFT = Duration.ofMinutes(5);

    /**
     * 验证签名
     * @param timestamp header: X-Timestamp (epoch seconds)
     * @param nonce header: X-Nonce
     * @param body 请求体
     * @param signature header: X-Signature
     * @return 是否验证通过
     */
    public boolean validate(String timestamp, String nonce, String body, String signature) {
        if (signature == null || signature.isEmpty()) {
            log.warn("Webhook signature is missing");
            return false;
        }
        if (timestamp == null || nonce == null) {
            log.warn("Webhook timestamp/nonce is missing");
            return false;
        }
        if (body == null) {
            log.warn("Webhook body is missing");
            return false;
        }

        try {
            long ts = Long.parseLong(timestamp);
            Instant sent = Instant.ofEpochSecond(ts);
            Instant now = Instant.now();
            if (Duration.between(sent, now).abs().compareTo(ALLOWED_DRIFT) > 0) {
                log.warn("Webhook timestamp out of allowed window, ts={}, now={}", sent, now);
                return false;
            }
        } catch (NumberFormatException ex) {
            log.warn("Webhook timestamp is not a valid epoch second: {}", timestamp);
            return false;
        }

        try {
            Mac hmac = Mac.getInstance("HmacSHA256");
            SecretKeySpec secretKey = new SecretKeySpec(appSecret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
            hmac.init(secretKey);
            String signingString = timestamp + "\n" + nonce + "\n" + body;
            byte[] hash = hmac.doFinal(signingString.getBytes(StandardCharsets.UTF_8));

            // Convert to Hex
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1)
                    hexString.append('0');
                hexString.append(hex);
            }
            String calculatedSignature = hexString.toString();

            log.debug("Signature check: received={}, calculated={}", signature, calculatedSignature);

            return signature.equalsIgnoreCase(calculatedSignature);
        } catch (Exception e) {
            log.error("Error validating signature", e);
            return false;
        }
    }
}
