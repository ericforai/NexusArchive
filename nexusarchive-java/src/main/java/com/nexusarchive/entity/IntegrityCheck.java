// Input: MyBatis-Plus、Lombok、Java 标准库
// Output: IntegrityCheck 四性检测结果实体
// Pos: 领域实体/模型
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

package com.nexusarchive.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * 四性检测结果实体
 *
 * 记录真实性、完整性、可用性、安全性检测结果。
 * 符合《电子档案管理基本术语》(DA/T 58-2014) 要求。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName(value = "integrity_check", autoResultMap = true)
public class IntegrityCheck {

    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 目标类型
     * BATCH: 归档批次
     * ARCHIVE: 档案
     * FILE: 文件
     * VOUCHER: 凭证
     */
    private String targetType;

    /**
     * 目标 ID
     */
    private Long targetId;

    /**
     * 检测类型
     * AUTHENTICITY: 真实性 - 来源可靠、内容真实
     * INTEGRITY: 完整性 - 内容完整、未被篡改
     * USABILITY: 可用性 - 可被正常读取和使用
     * SECURITY: 安全性 - 符合安全管理要求
     */
    private String checkType;

    /**
     * 检测结果
     * PASS: 通过
     * FAIL: 失败
     * WARNING: 警告
     */
    private String result;

    /**
     * 期望哈希值
     */
    private String hashExpected;

    /**
     * 实际哈希值
     */
    private String hashActual;

    /**
     * 签名是否有效
     */
    private Boolean signatureValid;

    /**
     * 检测详情（JSON）
     */
    @TableField(typeHandler = JacksonTypeHandler.class)
    private Map<String, Object> details;

    /**
     * 检测时间
     */
    private LocalDateTime checkedAt;

    /**
     * 检测人 ID
     */
    private Long checkedBy;

    // ========== 常量 ==========

    // 目标类型
    public static final String TARGET_BATCH = "BATCH";
    public static final String TARGET_ARCHIVE = "ARCHIVE";
    public static final String TARGET_FILE = "FILE";
    public static final String TARGET_VOUCHER = "VOUCHER";

    // 检测类型（四性）
    public static final String CHECK_AUTHENTICITY = "AUTHENTICITY";
    public static final String CHECK_INTEGRITY = "INTEGRITY";
    public static final String CHECK_USABILITY = "USABILITY";
    public static final String CHECK_SECURITY = "SECURITY";

    // 结果
    public static final String RESULT_PASS = "PASS";
    public static final String RESULT_FAIL = "FAIL";
    public static final String RESULT_WARNING = "WARNING";

    // ========== 便捷方法 ==========

    public boolean isPassed() {
        return RESULT_PASS.equals(result);
    }

    public boolean isFailed() {
        return RESULT_FAIL.equals(result);
    }

    public boolean hasWarning() {
        return RESULT_WARNING.equals(result);
    }

    public boolean isHashMatch() {
        return hashExpected != null && hashExpected.equals(hashActual);
    }
}
