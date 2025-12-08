package com.nexusarchive.integration.yonsuite.security;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;

/**
 * YonSuite Webhook 签名验证器
 * 验证规则: HmacSHA256(body, appSecret)
 */
@Component
@Slf4j
public class YonSuiteSignatureValidator {

    @Value("${yonsuite.app-secret}")
    private String appSecret;

    /**
     * 验证签名
     * @param body 请求体
     * @param signature 请求头中的签名
     * @return 是否验证通过
     */
    public boolean validate(String body, String signature) {
        if (signature == null || signature.isEmpty()) {
            log.warn("Webhook signature is missing");
            return false;
        }

        try {
            Mac hmac = Mac.getInstance("HmacSHA256");
            SecretKeySpec secretKey = new SecretKeySpec(appSecret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
            hmac.init(secretKey);
            byte[] hash = hmac.doFinal(body.getBytes(StandardCharsets.UTF_8));
            
            // Convert to Hex
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
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
