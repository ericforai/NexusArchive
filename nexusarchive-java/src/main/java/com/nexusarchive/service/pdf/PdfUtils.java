// Input: BouncyCastle SM3、Java 标准库
// Output: PdfUtils 工具类
// Pos: PDF 工具方法层
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

package com.nexusarchive.service.pdf;

import lombok.experimental.UtilityClass;
import org.bouncycastle.crypto.digests.SM3Digest;

/**
 * PDF 工具类
 * <p>
 * 提供 PDF 相关的工具方法：哈希计算、文本处理
 * </p>
 */
@UtilityClass
public class PdfUtils {

    /**
     * 计算 SM3 哈希值
     *
     * @param data 字节数据
     * @return 十六进制哈希字符串
     */
    public String calculateSM3Hash(byte[] data) {
        SM3Digest digest = new SM3Digest();
        digest.update(data, 0, data.length);
        byte[] hash = new byte[digest.getDigestSize()];
        digest.doFinal(hash, 0);

        StringBuilder sb = new StringBuilder();
        for (byte b : hash) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }

    /**
     * 截断文本
     *
     * @param text   原始文本
     * @param maxLen 最大长度
     * @return 截断后的文本
     */
    public String truncateText(String text, int maxLen) {
        if (text == null) {
            return "";
        }
        if (text.length() <= maxLen) {
            return text;
        }
        return text.substring(0, maxLen) + "...";
    }

    /**
     * 安全文本处理（中文支持）
     *
     * @param text           原始文本
     * @param supportChinese 是否支持中文
     * @return 处理后的文本
     */
    public String safeText(String text, boolean supportChinese) {
        if (text == null) {
            return "";
        }
        if (supportChinese) {
            return text;
        }

        // 回退到 ASCII 模式
        StringBuilder sb = new StringBuilder();
        for (char c : text.toCharArray()) {
            sb.append(c > 127 ? "?" : c);
        }
        return sb.toString();
    }

    /**
     * 格式化金额
     *
     * @param amount 金额
     * @return 格式化后的金额字符串
     */
    public String formatAmount(double amount) {
        return String.format("%.2f", amount);
    }

    /**
     * 格式化带币种的金额
     *
     * @param amount  金额
     * @param currency 币种代码
     * @return 格式化后的金额字符串
     */
    public String formatAmountWithCurrency(double amount, String currency) {
        return String.format("%.2f %s", amount, currency != null ? currency : "CNY");
    }
}
