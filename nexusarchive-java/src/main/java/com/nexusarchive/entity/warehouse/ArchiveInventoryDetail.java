// Input: MyBatis-Plus 注解、Lombok、JPA 规范
// Output: 盘点明细实体类
// Pos: src/main/java/com/nexusarchive/entity/warehouse
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

package com.nexusarchive.entity.warehouse;

import com.baomidou.mybatisplus.annotation.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 盘点明细实体类
 *
 * 核心职责：
 * 1. 记录每个档案袋的盘点结果
 * 2. 比对预期与实际状态
 * 3. 记录差异情况
 * 4. 关联盘点任务
 *
 * @author Claude Code
 * @since 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("archives_inventory_detail")
public class ArchiveInventoryDetail implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 主键ID
     */
    @TableId(value = "id", type = IdType.ASSIGN_ID)
    private Long id;

    /**
     * 盘点任务ID
     */
    @TableField("inventory_id")
    private Long inventoryId;

    /**
     * 档案袋ID
     */
    @TableField("container_id")
    private Long containerId;

    /**
     * 预期状态
     * normal - 正常
     * damaged - 已损坏
     * missing - 缺失
     */
    @TableField("expected_status")
    private String expectedStatus;

    /**
     * 实际状态
     * normal - 正常
     * damaged - 已损坏
     * missing - 缺失
     * extra - 多余
     */
    @TableField("actual_status")
    private String actualStatus;

    /**
     * 差异
     * matched - 一致
     * missing - 缺失
     * extra - 多余
     * damaged - 损坏
     */
    @TableField("difference")
    private String difference;

    /**
     * 备注
     */
    @TableField("remark")
    private String remark;

    /**
     * 所属全宗ID
     */
    @TableField("fonds_id")
    private Long fondsId;

    /**
     * 创建人ID
     */
    @TableField("created_by")
    private Long createdBy;

    /**
     * 创建时间
     */
    @TableField(value = "created_at", fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
