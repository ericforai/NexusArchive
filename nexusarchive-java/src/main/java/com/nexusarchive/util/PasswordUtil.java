package com.nexusarchive.util;

import de.mkammerer.argon2.Argon2;
import de.mkammerer.argon2.Argon2Factory;
import org.springframework.stereotype.Component;

/**
 * 密码工具类
 * 
 * 使用Argon2算法进行密码哈希
 * Argon2是2015年密码哈希竞赛的获胜者，比BCrypt更安全
 */
@Component
public class PasswordUtil {
    
    private static final Argon2 argon2 = Argon2Factory.create(
            Argon2Factory.Argon2Types.ARGON2id,
            32,  // salt length
            64   // hash length
    );
    
    /**
     * 哈希密码
     */
    public String hashPassword(String password) {
        return argon2.hash(3, 65536, 4, password.toCharArray());
    }
    
    /**
     * 验证密码
     */
    public boolean verifyPassword(String hash, String password) {
        return argon2.verify(hash, password.toCharArray());
    }
    
    /**
     * 清理Argon2资源
     */
    public void wipeArray(char[] array) {
        argon2.wipeArray(array);
    }
}
