// Input: Lombok、Java 标准库、本地模块
// Output: PdfSignatureVerificationResult 类
// Pos: 数据传输对象
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

package com.nexusarchive.dto.signature;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

/**
 * PDF 签名验证标准化结果
 */
@Getter
@Builder
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class PdfSignatureVerificationResult {

    private final PdfSignatureVerificationStatus status;
    private final Boolean signed;
    private final String message;
    private final String signerName;
    private final String certificateSubject;
    private final String certSerialNumber;
    private final String algorithm;
    private final LocalDateTime signTime;
    private final LocalDateTime verifiedAt;
    private final int signatureCount;

    public static PdfSignatureVerificationResult valid(
            int signatureCount,
            String message,
            String signerName,
            String certificateSubject,
            String certSerialNumber,
            String algorithm,
            LocalDateTime signTime) {
        return PdfSignatureVerificationResult.builder()
                .status(PdfSignatureVerificationStatus.VALID)
                .signed(Boolean.TRUE)
                .message(message)
                .signerName(signerName)
                .certificateSubject(certificateSubject)
                .certSerialNumber(certSerialNumber)
                .algorithm(algorithm)
                .signTime(signTime)
                .verifiedAt(LocalDateTime.now())
                .signatureCount(signatureCount)
                .build();
    }

    public static PdfSignatureVerificationResult invalid(
            int signatureCount,
            String message,
            String signerName,
            String certificateSubject,
            String certSerialNumber,
            String algorithm,
            LocalDateTime signTime) {
        return PdfSignatureVerificationResult.builder()
                .status(PdfSignatureVerificationStatus.INVALID)
                .signed(Boolean.TRUE)
                .message(message)
                .signerName(signerName)
                .certificateSubject(certificateSubject)
                .certSerialNumber(certSerialNumber)
                .algorithm(algorithm)
                .signTime(signTime)
                .verifiedAt(LocalDateTime.now())
                .signatureCount(signatureCount)
                .build();
    }

    public static PdfSignatureVerificationResult unknown(Boolean signed, int signatureCount, String message) {
        return PdfSignatureVerificationResult.builder()
                .status(PdfSignatureVerificationStatus.UNKNOWN)
                .signed(signed)
                .message(message)
                .verifiedAt(LocalDateTime.now())
                .signatureCount(signatureCount)
                .build();
    }
}
