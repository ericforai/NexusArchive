// Input: Lombok、Java 标准库
// Output: SignResult 类
// Pos: 数据传输对象
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

package com.nexusarchive.dto.signature;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 签章结果 DTO
 * 
 * 用于封装 SM2/RSA 等签名操作的结果
 */
@Data
@NoArgsConstructor
public class SignResult {

     public SignResult(boolean success, byte[] signature, String algorithm, String signerName, String certSerialNumber, String signerOrg, LocalDateTime signTime, String errorMessage) {
        this.success = success;
        this.signature = signature;
        this.algorithm = algorithm;
        this.signerName = signerName;
        this.certSerialNumber = certSerialNumber;
        this.signerOrg = signerOrg;
        this.signTime = signTime;
        this.errorMessage = errorMessage;
    }

    public static SignResultBuilder builder() {
        return new SignResultBuilder();
    }

    public static class SignResultBuilder {
        private boolean success;
        private byte[] signature;
        private String algorithm;
        private String signerName;
        private String certSerialNumber;
        private String signerOrg;
        private LocalDateTime signTime;
        private String errorMessage;

        SignResultBuilder() {}

        public SignResultBuilder success(boolean success) {
            this.success = success;
            return this;
        }

        public SignResultBuilder signature(byte[] signature) {
            this.signature = signature;
            return this;
        }

        public SignResultBuilder algorithm(String algorithm) {
            this.algorithm = algorithm;
            return this;
        }

        public SignResultBuilder signerName(String signerName) {
            this.signerName = signerName;
            return this;
        }

        public SignResultBuilder certSerialNumber(String certSerialNumber) {
            this.certSerialNumber = certSerialNumber;
            return this;
        }

        public SignResultBuilder signerOrg(String signerOrg) {
            this.signerOrg = signerOrg;
            return this;
        }

        public SignResultBuilder signTime(LocalDateTime signTime) {
            this.signTime = signTime;
            return this;
        }

        public SignResultBuilder errorMessage(String errorMessage) {
            this.errorMessage = errorMessage;
            return this;
        }

        public SignResult build() {
            return new SignResult(success, signature, algorithm, signerName, certSerialNumber, signerOrg, signTime, errorMessage);
        }
    }
    
    /**
     * 签名是否成功
     */
    private boolean success;
    
    /**
     * 签名值 (字节数组)
     */
    private byte[] signature;
    
    /**
     * 签名算法: SM2, RSA
     */
    private String algorithm;
    
    /**
     * 签章人姓名
     */
    private String signerName;
    
    /**
     * 证书序列号
     */
    private String certSerialNumber;
    
    /**
     * 签章单位
     */
    private String signerOrg;
    
    /**
     * 签章时间
     */
    private LocalDateTime signTime;
    
    /**
     * 错误信息 (失败时)
     */
    private String errorMessage;
    
    /**
     * 创建成功结果
     */
    public static SignResult success(byte[] signature, String algorithm, 
                                     String signerName, String certSerialNumber) {
        return SignResult.builder()
                .success(true)
                .signature(signature)
                .algorithm(algorithm)
                .signerName(signerName)
                .certSerialNumber(certSerialNumber)
                .signTime(LocalDateTime.now())
                .build();
    }
    
    /**
     * 创建失败结果
     */
    public static SignResult failure(String errorMessage) {
        return SignResult.builder()
                .success(false)
                .errorMessage(errorMessage)
                .build();
    }
}
