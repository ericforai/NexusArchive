// Input: MyBatis-Plus、Lombok、Java 标准库
// Output: VoucherRelation 类
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
 * 原始凭证与记账凭证关联关系实体
 * 对应表: arc_voucher_relation
 * 
 * 支持多对多关系：
 * - 一个原始凭证可关联多张记账凭证
 * - 一张记账凭证可关联多个原始凭证
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("arc_voucher_relation")
public class VoucherRelation {

    @TableId(type = IdType.ASSIGN_ID)
    private String id;

    /**
     * 原始凭证ID
     */
    private String originalVoucherId;

    /**
     * 记账凭证ID (arc_file_content.id)
     */
    private String accountingVoucherId;

    /**
     * 关系类型: ORIGINAL_TO_ACCOUNTING
     */
    @Builder.Default
    private String relationType = "ORIGINAL_TO_ACCOUNTING";

    /**
     * 关系说明
     */
    private String relationDesc;

    // ===== 审计字段 =====

    private String createdBy;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdTime;

    @TableLogic
    @Builder.Default
    private Integer deleted = 0;
}
