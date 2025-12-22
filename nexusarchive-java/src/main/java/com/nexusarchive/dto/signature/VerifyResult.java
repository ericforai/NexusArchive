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
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VerifyResult {
    
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
