// Input: Spring Framework、Java 标准库
// Output: XssFilter 类
// Pos: 工具模块
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

package com.nexusarchive.util;

import org.springframework.web.util.HtmlUtils;
import java.util.regex.Pattern;

/**
 * XSS 防护工具类
 * 
 * 用于过滤用户输入中的潜在 XSS 攻击代码
 * 
 * @author 安全模块
 */
public class XssFilter {
    
    // 危险标签的正则模式
    private static final Pattern SCRIPT_PATTERN = Pattern.compile(
        "<script[^>]*>.*?</script>", Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
    private static final Pattern SCRIPT_TAG_PATTERN = Pattern.compile(
        "<script[^>]*>", Pattern.CASE_INSENSITIVE);
    private static final Pattern SCRIPT_END_PATTERN = Pattern.compile(
        "</script>", Pattern.CASE_INSENSITIVE);
    private static final Pattern JAVASCRIPT_PATTERN = Pattern.compile(
        "javascript:", Pattern.CASE_INSENSITIVE);
    private static final Pattern VBSCRIPT_PATTERN = Pattern.compile(
        "vbscript:", Pattern.CASE_INSENSITIVE);
    private static final Pattern ONLOAD_PATTERN = Pattern.compile(
        "on\\w+\\s*=", Pattern.CASE_INSENSITIVE);
    private static final Pattern EXPRESSION_PATTERN = Pattern.compile(
        "expression\\s*\\(", Pattern.CASE_INSENSITIVE);
    private static final Pattern IFRAME_PATTERN = Pattern.compile(
        "<iframe[^>]*>.*?</iframe>", Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
    private static final Pattern OBJECT_PATTERN = Pattern.compile(
        "<object[^>]*>.*?</object>", Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
    private static final Pattern EMBED_PATTERN = Pattern.compile(
        "<embed[^>]*>", Pattern.CASE_INSENSITIVE);
    
    /**
     * 清理潜在的 XSS 攻击代码
     * 
     * @param input 用户输入
     * @return 清理后的安全字符串
     */
    public static String clean(String input) {
        if (input == null || input.isEmpty()) {
            return input;
        }
        
        String result = input;
        
        // 移除危险标签和属性
        result = SCRIPT_PATTERN.matcher(result).replaceAll("");
        result = SCRIPT_TAG_PATTERN.matcher(result).replaceAll("");
        result = SCRIPT_END_PATTERN.matcher(result).replaceAll("");
        result = JAVASCRIPT_PATTERN.matcher(result).replaceAll("");
        result = VBSCRIPT_PATTERN.matcher(result).replaceAll("");
        result = ONLOAD_PATTERN.matcher(result).replaceAll("");
        result = EXPRESSION_PATTERN.matcher(result).replaceAll("");
        result = IFRAME_PATTERN.matcher(result).replaceAll("");
        result = OBJECT_PATTERN.matcher(result).replaceAll("");
        result = EMBED_PATTERN.matcher(result).replaceAll("");
        
        return result.trim();
    }
    
    /**
     * HTML 转义（用于显示时）
     * 
     * @param input 用户输入
     * @return 转义后的字符串
     */
    public static String escape(String input) {
        if (input == null || input.isEmpty()) {
            return input;
        }
        return HtmlUtils.htmlEscape(input);
    }
    
    /**
     * 完全清理：先移除危险代码，再转义
     * 
     * @param input 用户输入
     * @return 安全的字符串
     */
    public static String sanitize(String input) {
        return escape(clean(input));
    }
    
    /**
     * 检查是否包含潜在 XSS 代码
     * 
     * @param input 用户输入
     * @return true 如果包含危险代码
     */
    public static boolean containsXss(String input) {
        if (input == null || input.isEmpty()) {
            return false;
        }
        
        return SCRIPT_PATTERN.matcher(input).find() ||
               SCRIPT_TAG_PATTERN.matcher(input).find() ||
               JAVASCRIPT_PATTERN.matcher(input).find() ||
               VBSCRIPT_PATTERN.matcher(input).find() ||
               ONLOAD_PATTERN.matcher(input).find() ||
               EXPRESSION_PATTERN.matcher(input).find() ||
               IFRAME_PATTERN.matcher(input).find() ||
               OBJECT_PATTERN.matcher(input).find() ||
               EMBED_PATTERN.matcher(input).find();
    }
}
