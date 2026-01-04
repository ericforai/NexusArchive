// Input: MyBatis-Plus、Lombok、Java 标准库
// Output: UserMfaConfig 类
// Pos: 领域实体/模型
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

package com.nexusarchive.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 用户MFA配置实体
 * 对应表: user_mfa_config
 */
@Data
@TableName("user_mfa_config")
public class UserMfaConfig {

    @TableId(type = IdType.ASSIGN_ID)
    private String id;

    /**
     * 用户ID
     */
    private String userId;

    /**
     * MFA是否启用
     */
    private Boolean mfaEnabled;

    /**
     * MFA类型: TOTP, SMS, EMAIL
     */
    private String mfaType;

    /**
     * TOTP密钥（加密存储）
     */
    private String secretKey;

    /**
     * 备用码（JSON格式，加密存储）
     */
    private String backupCodes;

    /**
     * 最后使用时间
     */
    private LocalDateTime lastUsedAt;

    /**
     * 创建时间
     */
    @TableField(value = "created_at", fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    /**
     * 更新时间
     */
    @TableField(value = "updated_at", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;

    /**
     * 逻辑删除标记
     */
    @TableLogic
    private Integer deleted;
}



