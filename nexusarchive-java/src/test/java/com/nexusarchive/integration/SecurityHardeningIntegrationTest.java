// Input: org.junit、Spring Framework、Spring Security、Javax、等
// Output: SecurityHardeningIntegrationTest 测试用例类
// Pos: 后端测试用例
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

package com.nexusarchive.integration;

import com.nexusarchive.integration.yonsuite.security.WebhookNonceStore;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;

/**
 * 覆盖未鉴权/假成功场景的集成测试。
 */
@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = {
        "yonsuite.app-secret=test-secret",
        "yonsuite.webhook.allowed-ips=127.0.0.1"
})
public class SecurityHardeningIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private WebhookNonceStore nonceStore;

    @MockBean
    private org.springframework.context.ApplicationEventPublisher eventPublisher;

    @MockBean
    private com.nexusarchive.service.AbnormalVoucherService abnormalVoucherService;

    @AfterEach
    void cleanup() {
        nonceStore.clear();
    }

    @Test
    void abnormal_without_token_should_401() throws Exception {
        mockMvc.perform(get("/v1/abnormal"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void webhook_without_token_should_401() throws Exception {
        mockMvc.perform(post("/integration/yonsuite/webhook")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"data\":{\"id\":\"1\"}}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser
    void webhook_invalid_signature_should_403() throws Exception {
        mockMvc.perform(post("/integration/yonsuite/webhook")
                        .with(req -> { req.setRemoteAddr("127.0.0.1"); return req; })
                        .header("X-Timestamp", String.valueOf(Instant.now().getEpochSecond()))
                        .header("X-Nonce", UUID.randomUUID().toString())
                        .header("X-Signature", "bad-signature")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"data\":{\"id\":\"1\"}}"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser
    void webhook_empty_body_should_400() throws Exception {
        mockMvc.perform(post("/integration/yonsuite/webhook")
                        .with(req -> { req.setRemoteAddr("127.0.0.1"); return req; })
                        .header("X-Timestamp", String.valueOf(Instant.now().getEpochSecond()))
                        .header("X-Nonce", UUID.randomUUID().toString())
                        .header("X-Signature", "any"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser
    void webhook_duplicate_nonce_should_409() throws Exception {
        String nonce = UUID.randomUUID().toString();
        String body = "{\"data\":{\"id\":\"1\"}}";
        String ts = String.valueOf(Instant.now().getEpochSecond());
        String sig = sign(ts, nonce, body, "test-secret");

        mockMvc.perform(post("/integration/yonsuite/webhook")
                        .with(req -> { req.setRemoteAddr("127.0.0.1"); return req; })
                        .header("X-Timestamp", ts)
                        .header("X-Nonce", nonce)
                        .header("X-Signature", sig)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk());

        mockMvc.perform(post("/integration/yonsuite/webhook")
                        .with(req -> { req.setRemoteAddr("127.0.0.1"); return req; })
                        .header("X-Timestamp", ts)
                        .header("X-Nonce", nonce)
                        .header("X-Signature", sig)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isConflict());
    }

    @Test
    @WithMockUser
    void webhook_internal_error_should_500() throws Exception {
        String nonce = UUID.randomUUID().toString();
        String body = "{\"data\":{\"id\":\"1\"}}";
        String ts = String.valueOf(Instant.now().getEpochSecond());
        String sig = sign(ts, nonce, body, "test-secret");

        doThrow(new RuntimeException("DB down")).when(eventPublisher).publishEvent(any());

        mockMvc.perform(post("/integration/yonsuite/webhook")
                        .with(req -> { req.setRemoteAddr("127.0.0.1"); return req; })
                        .header("X-Timestamp", ts)
                        .header("X-Nonce", nonce)
                        .header("X-Signature", sig)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isInternalServerError());
    }

    private String sign(String ts, String nonce, String body, String secret) throws Exception {
        Mac hmac = Mac.getInstance("HmacSHA256");
        SecretKeySpec secretKey = new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
        hmac.init(secretKey);
        byte[] hash = hmac.doFinal((ts + "\n" + nonce + "\n" + body).getBytes(StandardCharsets.UTF_8));
        StringBuilder hexString = new StringBuilder();
        for (byte b : hash) {
            String hex = Integer.toHexString(0xff & b);
            if (hex.length() == 1) hexString.append('0');
            hexString.append(hex);
        }
        return hexString.toString();
    }
}
