// Input: MyBatis-Plus、Lombok、Java 标准库
// Output: OriginalVoucherType 类
// Pos: 领域实体/模型
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

package com.nexusarchive.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDateTime;

/**
 * 原始凭证类型字典实体
 * 对应表: sys_original_voucher_type
 * 
 * 支持运维配置扩展，避免硬编码类型
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("sys_original_voucher_type")
public class OriginalVoucherType {

    @TableId(type = IdType.ASSIGN_ID)
    private String id;

    /**
     * 一级类型代码: INVOICE/BANK/DOCUMENT/CONTRACT/OTHER
     */
    private String categoryCode;

    /**
     * 一级类型名称
     */
    private String categoryName;

    /**
     * 二级类型代码 (唯一)
     */
    private String typeCode;

    /**
     * 二级类型名称
     */
    private String typeName;

    /**
     * 默认保管期限: 10Y/30Y/PERMANENT
     */
    @Builder.Default
    private String defaultRetention = "30Y";

    /**
     * 排序号
     */
    @Builder.Default
    private Integer sortOrder = 0;

    /**
     * 是否启用
     */
    @Builder.Default
    private Boolean enabled = true;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime lastModifiedTime;
}
