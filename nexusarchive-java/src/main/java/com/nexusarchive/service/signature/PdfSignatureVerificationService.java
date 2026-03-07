// Input: Java 标准库、本地模块
// Output: PdfSignatureVerificationService 接口
// Pos: 业务服务层
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

package com.nexusarchive.service.signature;

import com.nexusarchive.dto.signature.PdfSignatureVerificationResult;

import java.io.InputStream;

/**
 * PDF 签名验证服务
 */
public interface PdfSignatureVerificationService {

    /**
     * 验证 PDF 输入流中的数字签名。
     */
    PdfSignatureVerificationResult verify(InputStream pdfStream);

    /**
     * 验证 PDF 字节数组中的数字签名。
     */
    PdfSignatureVerificationResult verify(byte[] pdfBytes);
}
