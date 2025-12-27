// Input: MyBatis-Plus、Lombok、Java 标准库
// Output: PreArchiveStatus 枚举
// Pos: 领域实体/模型
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

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
     * 草稿 - 刚从ERP同步或手动录入，尚未开始处理
     */
    DRAFT("DRAFT", "草稿"),

    /**
     * 待检测 - 等待进行四性检测
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
     * 待匹配 - 检测通过，等待智能匹配原始凭证
     */
    MATCH_PENDING("MATCH_PENDING", "待匹配"),

    /**
     * 匹配成功 - 已自动关联到必要的原始凭证
     */
    MATCHED("MATCHED", "匹配成功"),

    /**
     * 准备归档 - 匹配完成且审核通过，等待提交归档
     */
    PENDING_ARCHIVE("PENDING_ARCHIVE", "准备归档"),
    
    /**
     * 归档审批中 - 已提交申请，等待审批
     */
    PENDING_APPROVAL("PENDING_APPROVAL", "归档审批中"),
    
    /**
     * 归档处理中 - 异步任务正在执行
     */
    ARCHIVING("ARCHIVING", "归档处理中"),
    
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
