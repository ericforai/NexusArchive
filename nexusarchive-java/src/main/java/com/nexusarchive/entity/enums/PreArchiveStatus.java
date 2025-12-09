package com.nexusarchive.entity.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import lombok.Getter;

/**
 * 预归档文件状态枚举
 * 根据《会计档案管理办法》第11条设计
 */
@Getter
public enum PreArchiveStatus {
    
    /**
     * 待检测 - 文件刚上传，等待四性检测
     */
    PENDING_CHECK("PENDING_CHECK", "待检测"),
    
    /**
     * 检测失败 - 四性检测未通过
     */
    CHECK_FAILED("CHECK_FAILED", "检测失败"),
    
    /**
     * 待补录 - 元数据不完整，需要补录
     */
    PENDING_METADATA("PENDING_METADATA", "待补录"),
    
    /**
     * 待归档 - 检测通过，等待审批归档
     */
    PENDING_ARCHIVE("PENDING_ARCHIVE", "待归档"),
    
    /**
     * 归档审批中 - 已提交申请，等待审批
     */
    PENDING_APPROVAL("PENDING_APPROVAL", "归档审批中"),
    
    /**
     * 已归档 - 正式归档完成
     */
    ARCHIVED("ARCHIVED", "已归档");
    
    @EnumValue
    private final String code;
    private final String description;
    
    PreArchiveStatus(String code, String description) {
        this.code = code;
        this.description = description;
    }
    
    /**
     * 根据code获取枚举
     */
    public static PreArchiveStatus fromCode(String code) {
        for (PreArchiveStatus status : values()) {
            if (status.getCode().equals(code)) {
                return status;
            }
        }
        return PENDING_CHECK;
    }
}
