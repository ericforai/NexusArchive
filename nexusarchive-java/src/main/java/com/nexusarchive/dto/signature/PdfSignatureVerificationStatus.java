// Input: Java 标准库
// Output: PdfSignatureVerificationStatus 枚举
// Pos: 数据传输对象
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

package com.nexusarchive.dto.signature;

/**
 * PDF 签名验证状态
 */
public enum PdfSignatureVerificationStatus {

    VALID("valid"),
    INVALID("invalid"),
    UNKNOWN("unknown");

    private final String code;

    PdfSignatureVerificationStatus(String code) {
        this.code = code;
    }

    public String getCode() {
        return code;
    }
}
