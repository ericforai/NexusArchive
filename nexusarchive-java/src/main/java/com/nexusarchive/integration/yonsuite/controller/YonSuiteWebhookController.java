package com.nexusarchive.integration.yonsuite.controller;

import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.nexusarchive.integration.yonsuite.event.YonSuiteVoucherEvent;
import com.yonyou.iuap.open.invoke.EventParamDecrypt;
import com.yonyou.iuap.open.utils.BeanJsonConvertUtil;
import com.yonyou.iuap.open.utils.crypto.EncryptionHolder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/integration/yonsuite/webhook")
@Slf4j
@RequiredArgsConstructor
public class YonSuiteWebhookController {

    private final ApplicationEventPublisher eventPublisher;

    @Value("${yonsuite.app-key:}")
    private String appKey;

    @Value("${yonsuite.app-secret:}")
    private String appSecret;

    @GetMapping
    public ResponseEntity<String> checkHealth() {
        log.info("Received YonSuite webhook health check (GET)");
        return ResponseEntity.ok("success");
    }

    @PostMapping
    public ResponseEntity<String> handleWebhook(@RequestBody(required = false) String body) {
        
        log.debug("Webhook body: {}", body);

        if (body == null) {
            log.warn("Received empty body");
            return ResponseEntity.ok("success");
        }

        try {
            JSONObject json = JSONUtil.parseObj(body);
            
            // 1. Check for Encryption
            String encrypt = json.getStr("encrypt");
            if (encrypt != null) {
                log.info("Received Encrypted Webhook. Attempting to decrypt using YonSuite SDK (Self-Built App)...");
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
                    // Return 200 to allow YS to mark as delivered (prevent retry storm)
                    return ResponseEntity.ok("success");
                }
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
            return ResponseEntity.ok("success");
        }
    }
}
