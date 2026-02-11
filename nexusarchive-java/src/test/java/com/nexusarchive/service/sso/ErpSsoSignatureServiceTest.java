package com.nexusarchive.service.sso;

import com.nexusarchive.exception.ErpSsoException;
import com.nexusarchive.service.sso.impl.ErpSsoSignatureServiceImpl;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ErpSsoSignatureServiceTest {

    private final ErpSsoSignatureService service = new ErpSsoSignatureServiceImpl();

    @Test
    void should_verify_signature_success() {
        String payload = "ERP_A|1739230000|nonce-a|BR01|1001|记-8";
        String secret = "secret-key";
        String signature = service.sign(payload, secret);

        assertTrue(service.verify(payload, secret, signature));
        assertFalse(service.verify(payload, secret, signature + "x"));
    }

    @Test
    void should_validate_timestamp_within_skew() {
        long now = 1_739_230_000L;
        assertDoesNotThrow(() -> service.validateTimestamp(now - 100, now, 300));
        assertDoesNotThrow(() -> service.validateTimestamp(now + 100, now, 300));
    }

    @Test
    void should_reject_expired_timestamp() {
        long now = 1_739_230_000L;
        assertThrows(ErpSsoException.class,
                () -> service.validateTimestamp(now - 301, now, 300));
    }
}
