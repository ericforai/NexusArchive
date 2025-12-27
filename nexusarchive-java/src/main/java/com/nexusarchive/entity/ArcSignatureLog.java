// Input: MyBatis-Plus、Lombok、Java 标准库
// Output: ArcSignatureLog 类
// Pos: 领域实体/模型
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

package com.nexusarchive.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 签章日志实体
 * 
 * 记录每次签章/验签操作的结果
 * 
 * 合规要求：
 * - DA/T 94-2022: 电子会计档案元数据规范
 * - 必须记录签章信息以便审计追溯
 * 
 * @author Agent B - 合规开发工程师
 */
@Data
@TableName("arc_signature_log")
public class ArcSignatureLog {
    
    @TableId(type = IdType.ASSIGN_ID)
    private String id;
    
    /**
     * 关联的档案ID
     */
    private String archiveId;
    
    /**
     * 关联的文件ID
     */
    private String fileId;
    
    /**
     * 签章人姓名
     */
    private String signerName;
    
    /**
     * 证书序列号
     */
    private String signerCertSn;
    
    /**
     * 签章单位
     */
    private String signerOrg;
    
    /**
     * 签章时间
     */
    private LocalDateTime signTime;
    
    /**
     * 签名算法: SM2, RSA
     */
    @TableField("sign_algorithm")
    private String signAlgorithm;
    
    /**
     * 签名值 (Base64 编码)
     */
    private String signatureValue;
    
    /**
     * 验证结果: VALID, INVALID, UNKNOWN
     */
    private String verifyResult;
    
    /**
     * 验证时间
     */
    private LocalDateTime verifyTime;
    
    /**
     * 验证消息 (错误信息或说明)
     */
    private String verifyMessage;
    
    /**
     * 创建时间
     */
    @TableField(value = "created_time", fill = FieldFill.INSERT)
    private LocalDateTime createdTime;
    
    /**
     * 验证结果枚举
     */
    public enum VerifyStatus {
        VALID("valid", "验证通过"),
        INVALID("invalid", "验证失败"),
        UNKNOWN("unknown", "未知状态");
        
        private final String code;
        private final String description;
        
        VerifyStatus(String code, String description) {
            this.code = code;
            this.description = description;
        }
        
        public String getCode() {
            return code;
        }
        
        public String getDescription() {
            return description;
        }
    }
}
