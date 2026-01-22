// Input: MyBatis-Plus、Lombok、Java 标准库
// Output: PeriodLock 期间锁定实体
// Pos: 领域实体/模型
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

package com.nexusarchive.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 期间锁定实体
 *
 * 控制会计期间的修改权限，与 ERP 结账状态对齐。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("period_lock")
public class PeriodLock {

    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 全宗 ID（公司/组织）
     */
    private String fondsId;

    /**
     * 期间，格式：2024-01
     */
    private String period;

    /**
     * 锁定类型
     * ERP_CLOSED: ERP 结账锁定
     * ARCHIVED: 归档锁定
     * AUDIT_LOCKED: 审计锁定
     */
    private String lockType;

    /**
     * 锁定时间
     */
    private LocalDateTime lockedAt;

    /**
     * 锁定人 ID
     */
    private String lockedBy;

    /**
     * 解锁时间（如果允许解锁）
     */
    private LocalDateTime unlockAt;

    /**
     * 解锁人 ID
     */
    private String unlockBy;

    /**
     * 锁定原因
     */
    private String reason;

    // ========== 常量 ==========

    public static final String TYPE_ERP_CLOSED = "ERP_CLOSED";
    public static final String TYPE_ARCHIVED = "ARCHIVED";
    public static final String TYPE_AUDIT_LOCKED = "AUDIT_LOCKED";

    // ========== 便捷方法 ==========

    public boolean isLocked() {
        return unlockAt == null;
    }
}
