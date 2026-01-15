// Input: Lombok、Java 标准库
// Output: VerifyResult 类
// Pos: 数据传输对象
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

package com.nexusarchive.dto.signature;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Date;

/**
 * 验签结果 DTO
 * 
 * 用于封装签章验证操作的结果
 */
@Data
@NoArgsConstructor
public class VerifyResult {

    public VerifyResult(boolean valid, boolean signatureValid, boolean certificateValid, boolean certificateExpired, String signerName, String signerOrg, String certSerialNumber, String certificateSubject, Date certificateExpiryDate, LocalDateTime signTime, LocalDateTime verifyTime, String algorithm, String errorMessage) {
        this.valid = valid;
        this.signatureValid = signatureValid;
        this.certificateValid = certificateValid;
        this.certificateExpired = certificateExpired;
        this.signerName = signerName;
        this.signerOrg = signerOrg;
        this.certSerialNumber = certSerialNumber;
        this.certificateSubject = certificateSubject;
        this.certificateExpiryDate = certificateExpiryDate;
        this.signTime = signTime;
        this.verifyTime = verifyTime;
        this.algorithm = algorithm;
        this.errorMessage = errorMessage;
    }

    public static VerifyResultBuilder builder() {
        return new VerifyResultBuilder();
    }

    public static class VerifyResultBuilder {
        private boolean valid;
        private boolean signatureValid;
        private boolean certificateValid;
        private boolean certificateExpired;
        private String signerName;
        private String signerOrg;
        private String certSerialNumber;
        private String certificateSubject;
        private Date certificateExpiryDate;
        private LocalDateTime signTime;
        private LocalDateTime verifyTime;
        private String algorithm;
        private String errorMessage;

        VerifyResultBuilder() {}

        public VerifyResultBuilder valid(boolean valid) {
            this.valid = valid;
            return this;
        }

        public VerifyResultBuilder signatureValid(boolean signatureValid) {
            this.signatureValid = signatureValid;
            return this;
        }

        public VerifyResultBuilder certificateValid(boolean certificateValid) {
            this.certificateValid = certificateValid;
            return this;
        }

        public VerifyResultBuilder certificateExpired(boolean certificateExpired) {
            this.certificateExpired = certificateExpired;
            return this;
        }

        public VerifyResultBuilder signerName(String signerName) {
            this.signerName = signerName;
            return this;
        }

        public VerifyResultBuilder signerOrg(String signerOrg) {
            this.signerOrg = signerOrg;
            return this;
        }

        public VerifyResultBuilder certSerialNumber(String certSerialNumber) {
            this.certSerialNumber = certSerialNumber;
            return this;
        }

        public VerifyResultBuilder certificateSubject(String certificateSubject) {
            this.certificateSubject = certificateSubject;
            return this;
        }

        public VerifyResultBuilder certificateExpiryDate(Date certificateExpiryDate) {
            this.certificateExpiryDate = certificateExpiryDate;
            return this;
        }

        public VerifyResultBuilder signTime(LocalDateTime signTime) {
            this.signTime = signTime;
            return this;
        }

        public VerifyResultBuilder verifyTime(LocalDateTime verifyTime) {
            this.verifyTime = verifyTime;
            return this;
        }

        public VerifyResultBuilder algorithm(String algorithm) {
            this.algorithm = algorithm;
            return this;
        }

        public VerifyResultBuilder errorMessage(String errorMessage) {
            this.errorMessage = errorMessage;
            return this;
        }

        public VerifyResult build() {
            return new VerifyResult(valid, signatureValid, certificateValid, certificateExpired, signerName, signerOrg, certSerialNumber, certificateSubject, certificateExpiryDate, signTime, verifyTime, algorithm, errorMessage);
        }
    }
    
    /**
     * 验证是否通过
     */
    private boolean valid;
    
    /**
     * 签名是否有效
     */
    private boolean signatureValid;
    
    /**
     * 证书是否有效
     */
    private boolean certificateValid;
    
    /**
     * 证书是否过期
     */
    private boolean certificateExpired;
    
    /**
     * 签章人姓名
     */
    private String signerName;
    
    /**
     * 签章单位
     */
    private String signerOrg;
    
    /**
     * 证书序列号
     */
    private String certSerialNumber;
    
    /**
     * 证书主题
     */
    private String certificateSubject;
    
    /**
     * 证书过期时间
     */
    private Date certificateExpiryDate;
    
    /**
     * 签章时间
     */
    private LocalDateTime signTime;
    
    /**
     * 验证时间
     */
    private LocalDateTime verifyTime;
    
    /**
     * 验证算法
     */
    private String algorithm;
    
    /**
     * 错误信息
     */
    private String errorMessage;
    
    /**
     * 创建成功结果
     */
    public static VerifyResult success(String signerName, String signerOrg, 
                                       String certSerialNumber, LocalDateTime signTime) {
        return VerifyResult.builder()
                .valid(true)
                .signatureValid(true)
                .certificateValid(true)
                .certificateExpired(false)
                .signerName(signerName)
                .signerOrg(signerOrg)
                .certSerialNumber(certSerialNumber)
                .signTime(signTime)
                .verifyTime(LocalDateTime.now())
                .build();
    }
    
    /**
     * 创建失败结果
     */
    public static VerifyResult failure(String errorMessage) {
        return VerifyResult.builder()
                .valid(false)
                .signatureValid(false)
                .errorMessage(errorMessage)
                .verifyTime(LocalDateTime.now())
                .build();
    }
    
    /**
     * 创建签名无效结果
     */
    public static VerifyResult invalidSignature(String message) {
        return VerifyResult.builder()
                .valid(false)
                .signatureValid(false)
                .certificateValid(true)
                .errorMessage(message != null ? message : "签名验证失败")
                .verifyTime(LocalDateTime.now())
                .build();
    }
    
    /**
     * 创建证书过期结果
     */
    public static VerifyResult certificateExpired(String signerName, Date expiryDate) {
        return VerifyResult.builder()
                .valid(false)
                .signatureValid(true)
                .certificateValid(false)
                .certificateExpired(true)
                .signerName(signerName)
                .certificateExpiryDate(expiryDate)
                .errorMessage("证书已过期")
                .verifyTime(LocalDateTime.now())
                .build();
    }
}
