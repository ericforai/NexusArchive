package com.nexusarchive.util;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class SM4UtilsTest {

    @Test
    public void testEncryptAndDecrypt() {
        String original = "HelloNexusArchive";
        String encrypted = SM4Utils.encrypt(original);
        assertNotNull(encrypted);
        assertNotEquals(original, encrypted);

        String decrypted = SM4Utils.decrypt(encrypted);
        assertEquals(original, decrypted);
    }

    @Test
    public void testDecryptStrictSuccess() {
        String original = "StrictSecret123";
        String encrypted = SM4Utils.encrypt(original);

        String decrypted = SM4Utils.decryptStrict(encrypted);
        assertEquals(original, decrypted);
    }

    @Test
    public void testDecryptStrictFailure() {
        // 使用非法的 hex 字符串或错误的密钥加密的数据
        String invalidHex = "ffffffffffffffffffffffffffffffff";

        assertThrows(RuntimeException.class, () -> {
            SM4Utils.decryptStrict(invalidHex);
        }, "SM4严格模式下解密失败必须抛出错误");
    }

    @Test
    public void testDecryptNonHex() {
        String nonHex = "NotAHexString";
        // 普通模式返回原样
        assertEquals(nonHex, SM4Utils.decrypt(nonHex));

        // 严格模式抛出异常
        assertThrows(RuntimeException.class, () -> {
            SM4Utils.decryptStrict(nonHex);
        });
    }
}
