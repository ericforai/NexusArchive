// Input: Database pre_archive_status column (VARCHAR)
// Output: PreArchiveStatus enum with 5 states
// Pos: src/main/java/com/nexusarchive/entity/enums/

package com.nexusarchive.entity.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Getter;

/**
 * 预归档状态枚举（简化版 - 5 个核心状态）
 *
 * <p>状态迁移说明：
 * <ul>
 *   <li>PENDING_CHECK: 合并 DRAFT + PENDING_CHECK</li>
 *   <li>NEEDS_ACTION: 合并 CHECK_FAILED + PENDING_METADATA</li>
 *   <li>READY_TO_MATCH: 合并 MATCH_PENDING + MATCHED</li>
 *   <li>READY_TO_ARCHIVE: 原 PENDING_ARCHIVE</li>
 *   <li>COMPLETED: 合并 PENDING_APPROVAL + ARCHIVING + ARCHIVED</li>
 * </ul>
 *
 * @see <a href="V96__simplify_pre_archive_status.sql">Flyway migration V96</a>
 */
@Getter
public enum PreArchiveStatus {

    /** 待检测 - 新导入或初始状态的凭证 */
    PENDING_CHECK("PENDING_CHECK", "待检测"),

    /** 待处理 - 检测失败或需要补全元数据 */
    NEEDS_ACTION("NEEDS_ACTION", "待处理"),

    /** 可匹配 - 可以进行凭证关联操作 */
    READY_TO_MATCH("READY_TO_MATCH", "可匹配"),

    /** 可归档 - 已就绪，可以提交归档（核心状态） */
    READY_TO_ARCHIVE("READY_TO_ARCHIVE", "可归档"),

    /** 已提交待审批 - 已提交归档申请，等待审批 */
    SUBMITTED("SUBMITTED", "已提交"),

    /** 已完成 - 归档流程已结束 */
    COMPLETED("COMPLETED", "已完成");

    /**
     * 旧状态到新状态的映射表（用于迁移兼容）
     */
    private static final java.util.Map<String, PreArchiveStatus> OLD_STATUS_MAP =
        java.util.Map.ofEntries(
            java.util.Map.entry("DRAFT", PENDING_CHECK),
            java.util.Map.entry("PENDING_CHECK", PENDING_CHECK),
            java.util.Map.entry("CHECK_FAILED", NEEDS_ACTION),
            java.util.Map.entry("PENDING_METADATA", NEEDS_ACTION),
            java.util.Map.entry("MATCH_PENDING", READY_TO_MATCH),
            java.util.Map.entry("MATCHED", READY_TO_MATCH),
            java.util.Map.entry("PENDING_ARCHIVE", READY_TO_ARCHIVE),
            java.util.Map.entry("PENDING_APPROVAL", COMPLETED),
            java.util.Map.entry("ARCHIVING", COMPLETED),
            java.util.Map.entry("ARCHIVED", COMPLETED)
        );

    @EnumValue
    @JsonValue
    private final String code;

    private final String description;

    PreArchiveStatus(String code, String description) {
        this.code = code;
        this.description = description;
    }

    /**
     * 从旧状态代码获取枚举值（迁移兼容）
     *
     * @param oldCode 旧状态代码（可能已废弃）
     * @return 对应的新状态枚举
     */
    public static PreArchiveStatus fromOldCode(String oldCode) {
        return OLD_STATUS_MAP.getOrDefault(oldCode, PENDING_CHECK);
    }

    /**
     * 从代码获取枚举值
     *
     * @param code 状态代码
     * @return 对应的枚举值
     * @throws IllegalArgumentException 如果代码无效
     */
    public static PreArchiveStatus fromCode(String code) {
        for (PreArchiveStatus status : values()) {
            if (status.code.equals(code)) {
                return status;
            }
        }
        throw new IllegalArgumentException("Unknown PreArchiveStatus: " + code);
    }

    /**
     * 检查状态是否为可归档
     */
    public boolean isReadyToArchive() {
        return this == READY_TO_ARCHIVE;
    }

    /**
     * 检查状态是否需要人工处理
     */
    public boolean needsAction() {
        return this == NEEDS_ACTION;
    }

    /**
     * 检查状态是否已完成
     */
    public boolean isCompleted() {
        return this == COMPLETED;
    }
}
