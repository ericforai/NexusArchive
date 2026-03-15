// Input: Spring Framework, Java 标准库
// Output: HttpHeaderUtils 类
// Pos: 工具模块
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

package com.nexusarchive.util;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.regex.Pattern;

/**
 * HTTP 响应头工具类
 *
 * 提供生成符合 HTTP 规范的响应头的静态方法
 */
public final class HttpHeaderUtils {

    private static final Pattern UNSAFE_FILENAME_CHARS = Pattern.compile("[^a-zA-Z0-9._\\-\\s]");

    private HttpHeaderUtils() {
        // Utility class
    }

    /**
     * 为 HTTP Content-Disposition 响应头编码文件名
     *
     * 使用 RFC 5987 格式支持非 ASCII 字符（如中文文件名）
     * 同时提供回退的 ASCII 文件名以保证兼容性
     *
     * @param fileName 原始文件名
     * @return Content-Disposition 头的值（不包含 "inline;" 或 "attachment;" 前缀）
     *
     * @see <a href="https://tools.ietf.org/html/rfc5987">RFC 5987</a>
     */
    public static String encodeFileNameForContentDisposition(String fileName) {
        if (fileName == null || fileName.isEmpty()) {
            return "filename=\"download\"";
        }

        // 安全过滤文件名作为回退（移除非 ASCII 字符）
        String safeName = UNSAFE_FILENAME_CHARS.matcher(fileName).replaceAll("_");

        // URL 编码支持非 ASCII 字符（RFC 5987 格式）
        String encodedName = URLEncoder.encode(fileName, StandardCharsets.UTF_8).replace("+", "%20");

        return String.format("filename=\"%s\"; filename*=UTF-8''%s", safeName, encodedName);
    }

    /**
     * 生成完整的 inline Content-Disposition 响应头
     *
     * @param fileName 文件名
     * @return 完整的 Content-Disposition 值
     */
    public static String inlineContentDisposition(String fileName) {
        return "inline; " + encodeFileNameForContentDisposition(fileName);
    }

    /**
     * 生成完整的 attachment Content-Disposition 响应头
     *
     * @param fileName 文件名
     * @return 完整的 Content-Disposition 值
     */
    public static String attachmentContentDisposition(String fileName) {
        return "attachment; " + encodeFileNameForContentDisposition(fileName);
    }
}
