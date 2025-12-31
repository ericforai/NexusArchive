// Input: 签名验证结果数据（含时间戳/链路状态）
// Output: 验证结果 DTO
// Pos: NexusCore compliance/signature
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

package com.nexusarchive.core.compliance;

import java.time.LocalDateTime;

/**
 * 签名验证结果
 */
public record SignatureVerifyResult(
    boolean valid,
    boolean signaturePresent,
    boolean timestampValid,
    boolean timestampPresent,
    String algorithm,          // SM2, RSA, etc.
    String signerName,
    LocalDateTime signTime,
    String certSerialNo,
    String timestampMessage,
    String errorMessage
) {
    public static SignatureVerifyResult success(String algorithm, String signerName,
            LocalDateTime signTime, String certSerialNo) {
        return successWithTimestamp(algorithm, signerName, signTime, certSerialNo, true, true, "时间戳已校验");
    }

    public static SignatureVerifyResult successWithTimestamp(String algorithm, String signerName,
            LocalDateTime signTime, String certSerialNo, boolean timestampValid, boolean timestampPresent,
            String timestampMessage) {
        return new SignatureVerifyResult(true, true, timestampValid, timestampPresent,
                algorithm, signerName, signTime, certSerialNo, timestampMessage, null);
    }

    public static SignatureVerifyResult failure(String errorMessage) {
        return failureWithTimestamp(errorMessage, true, false, false, null);
    }

    public static SignatureVerifyResult failureWithTimestamp(String errorMessage, boolean signaturePresent,
            boolean timestampValid, boolean timestampPresent, String timestampMessage) {
        return new SignatureVerifyResult(false, signaturePresent, timestampValid, timestampPresent,
                null, null, null, null, timestampMessage, errorMessage);
    }

    public static SignatureVerifyResult noSignature() {
        return new SignatureVerifyResult(false, false, false, false,
                null, null, null, null, null, "文件未包含数字签名");
    }

    public boolean complianceValid() {
        return valid && timestampValid;
    }
}
