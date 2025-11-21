package com.nexusarchive.util;

import de.mkammerer.argon2.Argon2;
import de.mkammerer.argon2.Argon2Factory;

/**
 * 密码哈希生成工具
 * 用于生成admin用户的初始密码
 */
public class PasswordHashGenerator {
    
    public static void main(String[] args) {
        Argon2 argon2 = Argon2Factory.create(
                Argon2Factory.Argon2Types.ARGON2id,
                32,
                64
        );
        
        String password = "admin123";
        String hash = argon2.hash(3, 65536, 4, password.toCharArray());
        
        System.out.println("Password: " + password);
        System.out.println("Hash: " + hash);
        System.out.println("\nSQL Update:");
        System.out.println("UPDATE sys_user SET password_hash = '" + hash + "' WHERE username = 'admin';");
    }
}
