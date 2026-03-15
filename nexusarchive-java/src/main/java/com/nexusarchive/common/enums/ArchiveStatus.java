// Input: Jackson、Lombok、Java 标准库
// Output: ArchiveStatus 枚举
// Pos: common/enums

package com.nexusarchive.common.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Getter;

/**
 * 档案状态枚举
 * <p>状态转换规则:
 * <ul>
 *   <li>DRAFT -> PENDING: 提交审核</li>
 *   <li>PENDING -> ARCHIVED: 审核通过</li>
 *   <li>PENDING -> DRAFT: 审核拒绝</li>
 *   <li>ARCHIVED: 终态，不可转换</li>
 * </ul>
 *
 * @see <a href="https://dat94.github.io/DA-T-94-2022">DA/T 94-2022 电子会计档案管理规范</a>
 */
@Getter
public enum ArchiveStatus {

    /**
     * 草稿 - 新创建的档案
     */
    DRAFT("draft", "草稿"),

    /**
     * 待审核 - 已提交，等待审批
     */
    PENDING("pending", "待审核"),

    /**
     * 已归档 - 正式归档完成（终态）
     */
    ARCHIVED("archived", "已归档");

    private final String code;
    private final String description;

    ArchiveStatus(String code, String description) {
        this.code = code;
        this.description = description;
    }

    @JsonValue
    public String getCode() {
        return code;
    }

    /**
     * 从代码解析枚举值
     * <p>支持大小写不敏感解析
     * <p>null 或空字符串返回默认值 DRAFT
     *
     * @param code 状态代码
     * @return 对应的枚举值
     * @throws IllegalArgumentException 如果代码无效
     */
    @JsonCreator
    public static ArchiveStatus fromCode(String code) {
        if (code == null || code.isBlank()) {
            return DRAFT; // 默认为草稿
        }
        String normalizedCode = code.trim().toLowerCase();
        for (ArchiveStatus status : values()) {
            if (status.code.equals(normalizedCode)) {
                return status;
            }
        }
        throw new IllegalArgumentException("Invalid archive status: " + code +
            ". Must be one of: draft, pending, archived");
    }

    /**
     * 检查是否可以转换到目标状态
     *
     * @param target 目标状态
     * @return 是否可以转换
     */
    public boolean canTransitionTo(ArchiveStatus target) {
        if (target == null) {
            return false;
        }

        return switch (this) {
            case DRAFT -> target == PENDING;
            case PENDING -> target == DRAFT || target == ARCHIVED;
            case ARCHIVED -> false; // 终态，不可转换
        };
    }

    /**
     * 判断是否为终态
     *
     * @return 是否为终态
     */
    public boolean isTerminal() {
        return this == ARCHIVED;
    }
}
