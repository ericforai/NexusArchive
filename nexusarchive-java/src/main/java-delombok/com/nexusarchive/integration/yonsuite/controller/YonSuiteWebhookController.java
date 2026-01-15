// Input: cn.hutool、Java 标准库、本地模块
// Output: YonSuiteWebhookController 类
// Pos: 接口层 Controller
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

package com.nexusarchive.integration.yonsuite.controller;

import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.nexusarchive.integration.yonsuite.event.YonSuiteVoucherEvent;
import com.nexusarchive.integration.yonsuite.security.WebhookNonceStore;
// import com.yonyou.iuap.open.invoke.EventParamDecrypt;
// import com.yonyou.iuap.open.utils.BeanJsonConvertUtil;
// import com.yonyou.iuap.open.utils.crypto.EncryptionHolder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@RestController
@RequestMapping("/integration/yonsuite/webhook")
@Slf4j
@RequiredArgsConstructor
public class YonSuiteWebhookController {

    private final ApplicationEventPublisher eventPublisher;
    private final com.nexusarchive.integration.yonsuite.security.YonSuiteSignatureValidator signatureValidator;
    private final WebhookNonceStore nonceStore;

    @Value("${yonsuite.app-key:}")
    private String appKey;

    @Value("${yonsuite.app-secret:}")
    private String appSecret;

    @Value("${yonsuite.webhook.allowed-ips:127.0.0.1,::1}")
    private List<String> allowedIps;

    @PostMapping
    public ResponseEntity<String> handleWebhook(
            @RequestHeader(value = "X-Timestamp", required = false) String timestamp,
            @RequestHeader(value = "X-Nonce", required = false) String nonce,
            @RequestHeader(value = "X-Signature", required = false) String signature,
            @RequestBody(required = false) String body,
            HttpServletRequest request) {

        if (!ipAllowed(request.getRemoteAddr())) {
            log.warn("Webhook request blocked by IP whitelist, ip={}", request.getRemoteAddr());
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("forbidden");
        }

        if (body == null || body.isBlank()) {
            log.warn("Received empty body");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("body required");
        }

        if (!signatureValidator.validate(timestamp, nonce, body, signature)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("invalid signature");
        }

        long ts;
        try {
            ts = Long.parseLong(timestamp);
        } catch (NumberFormatException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("invalid timestamp");
        }

        if (!nonceStore.registerIfNew(nonce, ts)) {
            log.warn("Duplicate nonce detected: {}", nonce);
            return ResponseEntity.status(HttpStatus.CONFLICT).body("replay detected");
        }

        log.debug("Webhook body: {}", body);

        try {
            JSONObject json = JSONUtil.parseObj(body);
            
            // 1. Check for Encryption
            String encrypt = json.getStr("encrypt");
            if (encrypt != null) {
                log.warn("Received Encrypted Webhook, but YonSuite SDK is currently DISABLED in pom.xml. Please restore the SDK to enable decryption.");
                return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED).body("decryption disabled (missing SDK)");
                /*
                try {
                    // 1. Convert JSON string to SDK's EncryptionHolder
                    EncryptionHolder holder = BeanJsonConvertUtil.jsonToBean(body, EncryptionHolder.class);
                    
                    // 2. Decrypt using selfAppParamDecrypt (for Self-Built App)
                    // Signature: (EncryptionHolder holder, String appKey, String appSecret)
                    com.yonyou.iuap.open.module.EventContent content = EventParamDecrypt.selfAppParamDecrypt(holder, appKey, appSecret);
                    
                    log.info("Decrypted Content raw: {}", content);
                    
                    // The 'content' object usually has the actual data fields. 
                    // To unify downstream processing, we convert the 'content' bean back to a JSON Object.
                    // This JSON object effectively replaces the encrypted wrapper.
                    String decryptedJsonStr = BeanJsonConvertUtil.beanToJson(content);
                    log.info("Decrypted JSON String: {}", decryptedJsonStr);
                    
                    json = JSONUtil.parseObj(decryptedJsonStr);
                    
                } catch (Exception e) {
                    log.error("SDK Decryption Failed!", e);
                    return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("decrypt failed");
                }
                */
            } else {
                 log.info("Received Plaintext Webhook.");
            }

            // 2. Process Data
            // Try to find voucher info
            // Notes: 
            // - Standard events might be in a "data" property or at root.
            // - Tally (Audit) events might specifically have 'voucherId'.
            JSONObject data = json.getJSONObject("data");
            if (data == null) {
                // If no "data" wrapper, treat root as data
                data = json; 
            }

            if (data != null) {
                 if (data.containsKey("id")) {
                    String voucherId = data.getStr("id");
                    String orgId = data.getStr("orgId");
                    if (voucherId != null) {
                        eventPublisher.publishEvent(new YonSuiteVoucherEvent(this, voucherId, orgId));
                        log.info("Published sync event for voucher: {}", voucherId);
                    }
                }
                else if (data.containsKey("voucherId")) {
                    String voucherIdStr = data.getStr("voucherId");
                    log.info("Processing Tally Event with voucherIds: {}", voucherIdStr);
                    try {
                         if (voucherIdStr != null && voucherIdStr.startsWith("[")) {
                             cn.hutool.json.JSONArray ids = JSONUtil.parseArray(voucherIdStr);
                             String orgId = data.getStr("orgId");
                             for (Object id : ids) {
                                 if (id != null) {
                                     eventPublisher.publishEvent(new YonSuiteVoucherEvent(this, id.toString(), orgId));
                                     log.info("Published sync event for voucher (from Tally): {}", id);
                                 }
                             }
                         } else if (voucherIdStr != null) {
                             eventPublisher.publishEvent(new YonSuiteVoucherEvent(this, voucherIdStr, data.getStr("orgId")));
                         }
                    } catch (Exception e) {
                         log.warn("Failed to parse voucherId: {}", voucherIdStr);
                         // Try fallback as single string
                         if (voucherIdStr != null) {
                            eventPublisher.publishEvent(new YonSuiteVoucherEvent(this, voucherIdStr, data.getStr("orgId")));
                         }
                    }
                }
            }
            
            return ResponseEntity.ok("success");
        } catch (Exception e) {
            log.error("Error parsing webhook body", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("internal error");
        }
    }

    private boolean ipAllowed(String remoteAddr) {
        if (remoteAddr == null) return false;
        Set<String> wl = new HashSet<>();
        for (String ip : allowedIps) {
            wl.add(ip.trim());
        }
        return wl.contains(remoteAddr);
    }
}
