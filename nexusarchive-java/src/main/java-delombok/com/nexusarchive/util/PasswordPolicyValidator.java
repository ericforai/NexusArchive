// Input: Java 标准库、本地模块
// Output: PasswordPolicyValidator 类
// Pos: 工具模块
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

package com.nexusarchive.util;

import com.nexusarchive.common.exception.BusinessException;
import com.nexusarchive.common.exception.ErrorCode;

import java.util.regex.Pattern;

/**
 * 简单密码策略校验：长度>=8，包含大小写字母、数字、特殊字符
 */
public class PasswordPolicyValidator {

    private static final Pattern UPPER = Pattern.compile("[A-Z]");
    private static final Pattern LOWER = Pattern.compile("[a-z]");
    private static final Pattern DIGIT = Pattern.compile("\\d");
    private static final Pattern SPECIAL = Pattern.compile("[^A-Za-z0-9]");

    public static void validate(String password) {
        if (password == null || password.length() < 8) {
            throw new BusinessException(ErrorCode.PASSWORD_TOO_SHORT);
        }
        if (!UPPER.matcher(password).find()) {
            throw new BusinessException(ErrorCode.PASSWORD_MISSING_UPPERCASE);
        }
        if (!LOWER.matcher(password).find()) {
            throw new BusinessException(ErrorCode.PASSWORD_MISSING_LOWERCASE);
        }
        if (!DIGIT.matcher(password).find()) {
            throw new BusinessException(ErrorCode.PASSWORD_MISSING_DIGIT);
        }
        if (!SPECIAL.matcher(password).find()) {
            throw new BusinessException(ErrorCode.PASSWORD_MISSING_SPECIAL_CHAR);
        }
    }
}
