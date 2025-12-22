// Input: Lombok、Spring Framework、Java 标准库
// Output: PasswordPolicyValidator 类
// Pos: 业务服务层
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

package com.nexusarchive.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * 密码策略验证器
 * 安全加固 - 强制密码复杂度要求
 * 
 * 默认策略：
 * - 最小长度 8 位
 * - 必须包含大写字母
 * - 必须包含小写字母
 * - 必须包含数字
 * - 必须包含特殊字符
 */
@Component
@Slf4j
public class PasswordPolicyValidator {

    @Value("${security.password.min-length:8}")
    private int minLength;

    @Value("${security.password.require-uppercase:true}")
    private boolean requireUppercase;

    @Value("${security.password.require-lowercase:true}")
    private boolean requireLowercase;

    @Value("${security.password.require-digit:true}")
    private boolean requireDigit;

    @Value("${security.password.require-special:true}")
    private boolean requireSpecial;

    @Value("${security.password.special-chars:@$!%*?&#^()_+-=}")
    private String specialChars;

    // 常见弱密码列表
    private static final List<String> WEAK_PASSWORDS = List.of(
        "password", "123456", "12345678", "qwerty", "abc123",
        "password1", "admin", "letmein", "welcome", "monkey",
        "master", "dragon", "111111", "baseball", "iloveyou",
        "trustno1", "sunshine", "princess", "admin123", "root"
    );

    /**
     * 验证密码是否符合策略
     * @param password 待验证的密码
     * @throws IllegalArgumentException 如果密码不符合策略
     */
    public void validate(String password) {
        List<String> violations = getViolations(password);
        if (!violations.isEmpty()) {
            throw new IllegalArgumentException("密码不符合安全策略：" + String.join("、", violations));
        }
    }

    /**
     * 检查密码是否符合策略
     * @param password 待检查的密码
     * @return true 如果密码符合策略
     */
    public boolean isValid(String password) {
        return getViolations(password).isEmpty();
    }

    /**
     * 获取密码违规项列表
     * @param password 待检查的密码
     * @return 违规项描述列表
     */
    public List<String> getViolations(String password) {
        List<String> violations = new ArrayList<>();

        if (password == null || password.isEmpty()) {
            violations.add("密码不能为空");
            return violations;
        }

        // 长度检查
        if (password.length() < minLength) {
            violations.add("密码长度至少 " + minLength + " 位");
        }

        // 大写字母检查
        if (requireUppercase && !Pattern.compile("[A-Z]").matcher(password).find()) {
            violations.add("必须包含大写字母");
        }

        // 小写字母检查
        if (requireLowercase && !Pattern.compile("[a-z]").matcher(password).find()) {
            violations.add("必须包含小写字母");
        }

        // 数字检查
        if (requireDigit && !Pattern.compile("[0-9]").matcher(password).find()) {
            violations.add("必须包含数字");
        }

        // 特殊字符检查
        if (requireSpecial) {
            String escapedChars = Pattern.quote(specialChars);
            if (!Pattern.compile("[" + escapedChars + "]").matcher(password).find()) {
                violations.add("必须包含特殊字符（" + specialChars + "）");
            }
        }

        // 常见弱密码检查
        if (WEAK_PASSWORDS.stream().anyMatch(weak -> 
                password.toLowerCase().contains(weak))) {
            violations.add("不能使用常见弱密码");
        }

        // 连续字符检查（如 abc, 123）
        if (hasSequentialChars(password, 4)) {
            violations.add("不能包含4个以上连续字符（如 abcd, 1234）");
        }

        // 重复字符检查（如 aaaa）
        if (hasRepeatedChars(password, 4)) {
            violations.add("不能包含4个以上重复字符（如 aaaa）");
        }

        return violations;
    }

    /**
     * 获取密码强度评分（0-100）
     */
    public int getStrengthScore(String password) {
        if (password == null || password.isEmpty()) {
            return 0;
        }

        int score = 0;

        // 长度分数（最高 30 分）
        score += Math.min(30, password.length() * 3);

        // 字符类型分数（每种 15 分，最高 60 分）
        if (Pattern.compile("[A-Z]").matcher(password).find()) score += 15;
        if (Pattern.compile("[a-z]").matcher(password).find()) score += 15;
        if (Pattern.compile("[0-9]").matcher(password).find()) score += 15;
        if (Pattern.compile("[^A-Za-z0-9]").matcher(password).find()) score += 15;

        // 扣分项
        if (hasSequentialChars(password, 3)) score -= 10;
        if (hasRepeatedChars(password, 3)) score -= 10;
        if (WEAK_PASSWORDS.stream().anyMatch(weak -> 
                password.toLowerCase().contains(weak))) {
            score -= 30;
        }

        return Math.max(0, Math.min(100, score));
    }

    /**
     * 获取密码强度等级
     */
    public PasswordStrength getStrength(String password) {
        int score = getStrengthScore(password);
        if (score >= 80) return PasswordStrength.STRONG;
        if (score >= 60) return PasswordStrength.MEDIUM;
        if (score >= 40) return PasswordStrength.WEAK;
        return PasswordStrength.VERY_WEAK;
    }

    /**
     * 检查是否有连续字符
     */
    private boolean hasSequentialChars(String password, int threshold) {
        if (password.length() < threshold) return false;
        
        int sequential = 1;
        for (int i = 1; i < password.length(); i++) {
            if (password.charAt(i) == password.charAt(i - 1) + 1) {
                sequential++;
                if (sequential >= threshold) return true;
            } else {
                sequential = 1;
            }
        }
        return false;
    }

    /**
     * 检查是否有重复字符
     */
    private boolean hasRepeatedChars(String password, int threshold) {
        if (password.length() < threshold) return false;
        
        int repeated = 1;
        for (int i = 1; i < password.length(); i++) {
            if (password.charAt(i) == password.charAt(i - 1)) {
                repeated++;
                if (repeated >= threshold) return true;
            } else {
                repeated = 1;
            }
        }
        return false;
    }

    /**
     * 密码强度枚举
     */
    public enum PasswordStrength {
        VERY_WEAK("非常弱"),
        WEAK("弱"),
        MEDIUM("中等"),
        STRONG("强");

        private final String description;

        PasswordStrength(String description) {
            this.description = description;
        }

        public String getDescription() {
            return description;
        }
    }
}
