// Input: JUnit + BouncyCastle
// Output: SM3 哈希服务单元测试
// Pos: NexusCore tests
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

package com.nexusarchive.core.compliance;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class Sm3HashServiceTests {
    private final Sm3HashService service = new Sm3HashService();

    @Test
    void shouldComputeSm3Hash() {
        String hash = service.hashSm3("hello");
        assertNotNull(hash);
        assertEquals(64, hash.length());
    }

    @Test
    void shouldComputeSha256Hash() {
        String hash = service.hashSha256("hello".getBytes());
        assertNotNull(hash);
        assertEquals(64, hash.length());
    }

    @Test
    void shouldProduceDifferentHashesForDifferentInputs() {
        String hash1 = service.hashSm3("hello");
        String hash2 = service.hashSm3("world");
        assertNotEquals(hash1, hash2);
    }

    @Test
    void shouldProduceConsistentHashes() {
        String hash1 = service.hashSm3("test");
        String hash2 = service.hashSm3("test");
        assertEquals(hash1, hash2);
    }

    @Test
    void shouldSupportAlgorithmSwitch() {
        byte[] data = "test".getBytes();
        String sm3Hash = service.hash(data, HashAlgorithm.SM3);
        String sha256Hash = service.hash(data, HashAlgorithm.SHA256);
        assertNotEquals(sm3Hash, sha256Hash);
    }
}
