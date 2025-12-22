// Input: de.mkammerer、Spring Framework、Java 标准库
// Output: PasswordUtil 类
// Pos: 工具模块
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

package com.nexusarchive.util;

import de.mkammerer.argon2.Argon2;
import de.mkammerer.argon2.Argon2Factory;
import org.springframework.stereotype.Component;
import com.nexusarchive.common.exception.BusinessException;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * 密码工具类
 * 
 * 使用Argon2算法进行密码哈希
 * [FIXED P1-5] 添加密码强度校验，符合等保 2.0 三级要求
 */
@Component
public class PasswordUtil {
    
    private static final Argon2 argon2 = Argon2Factory.create(
            Argon2Factory.Argon2Types.ARGON2id,
            32,  // salt length
            64   // hash length
    );
    
    // [ADDED P1-5] 密码策略常量
    private static final int MIN_LENGTH = 8;
    private static final int MAX_LENGTH = 128;
    private static final Pattern HAS_UPPERCASE = Pattern.compile("[A-Z]");
    private static final Pattern HAS_LOWERCASE = Pattern.compile("[a-z]");
    private static final Pattern HAS_DIGIT = Pattern.compile("[0-9]");
    private static final Pattern HAS_SPECIAL = Pattern.compile("[!@#$%^&*()_+\\-=\\[\\]{};':\"\\\\|,.<>/?]");
    
    // 常见弱密码黑名单
    private static final java.util.Set<String> WEAK_PASSWORDS = java.util.Set.of(
        "password", "123456", "12345678", "qwerty", "abc123", 
        "monkey", "1234567", "letmein", "trustno1", "dragon",
        "baseball", "iloveyou", "master", "sunshine", "ashley",
        "passw0rd", "admin123", "root123", "test123", "guest"
    );
    
    /**
     * [ADDED P1-5] 验证密码强度
     * 依据：等保 2.0 三级 第 7.1.2.2 条
     * 
     * @param password 明文密码
     * @throws BusinessException 如果密码不符合策略
     */
    public void validatePasswordStrength(String password) {
        List<String> errors = new ArrayList<>();
        
        if (password == null || password.isEmpty()) {
            throw new BusinessException(400, "密码不能为空");
        }
        
        // 长度检查
        if (password.length() < MIN_LENGTH) {
            errors.add("密码长度至少 " + MIN_LENGTH + " 位");
        }
        if (password.length() > MAX_LENGTH) {
            errors.add("密码长度不能超过 " + MAX_LENGTH + " 位");
        }
        
        // 复杂度检查：至少包含三种字符类型
        int complexity = 0;
        if (HAS_UPPERCASE.matcher(password).find()) complexity++;
        if (HAS_LOWERCASE.matcher(password).find()) complexity++;
        if (HAS_DIGIT.matcher(password).find()) complexity++;
        if (HAS_SPECIAL.matcher(password).find()) complexity++;
        
        if (complexity < 3) {
            errors.add("密码必须包含大写字母、小写字母、数字、特殊字符中的至少三种");
        }
        
        // 弱密码检查
        if (WEAK_PASSWORDS.contains(password.toLowerCase())) {
            errors.add("密码过于简单，请使用更复杂的密码");
        }
        
        // 连续字符检查（如 123, abc）
        if (hasConsecutiveChars(password, 4)) {
            errors.add("密码不能包含4个或更多连续字符（如1234或abcd）");
        }
        
        // 重复字符检查（如 aaaa）
        if (hasRepeatingChars(password, 4)) {
            errors.add("密码不能包含4个或更多重复字符（如aaaa）");
        }
        
        if (!errors.isEmpty()) {
            throw new BusinessException(400, "密码不符合安全策略：" + String.join("；", errors));
        }
    }
    
    /**
     * 检测连续字符
     */
    private boolean hasConsecutiveChars(String password, int length) {
        if (password.length() < length) return false;
        
        for (int i = 0; i <= password.length() - length; i++) {
            boolean isConsecutive = true;
            for (int j = 1; j < length; j++) {
                if (password.charAt(i + j) != password.charAt(i + j - 1) + 1) {
                    isConsecutive = false;
                    break;
                }
            }
            if (isConsecutive) return true;
        }
        return false;
    }
    
    /**
     * 检测重复字符
     */
    private boolean hasRepeatingChars(String password, int length) {
        if (password.length() < length) return false;
        
        for (int i = 0; i <= password.length() - length; i++) {
            boolean isRepeating = true;
            char c = password.charAt(i);
            for (int j = 1; j < length; j++) {
                if (password.charAt(i + j) != c) {
                    isRepeating = false;
                    break;
                }
            }
            if (isRepeating) return true;
        }
        return false;
    }
    
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

