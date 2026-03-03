// Input: MyBatis-Plus 注解、Lombok、JPA 规范
// Output: 盘点任务实体类
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
 * 盘点任务实体类
 *
 * 核心职责：
 * 1. 管理档案盘点任务和执行过程
 * 2. 支持按档案柜范围盘点
 * 3. 记录盘点进度和结果统计
 * 4. 任务状态流转管理
 *
 * @author Claude Code
 * @since 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("archives_inventory")
public class ArchiveInventory implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 主键ID
     */
    @TableId(value = "id", type = IdType.ASSIGN_ID)
    private Long id;

    /**
     * 任务号
     * 格式：PD-YYYY-NNN
     */
    @TableField("task_no")
    private String taskNo;

    /**
     * 任务名称
     */
    @TableField("task_name")
    private String taskName;

    /**
     * 盘点档案柜ID
     * 可为空，空=全库盘点
     */
    @TableField("cabinet_id")
    private Long cabinetId;

    /**
     * 起始柜号
     */
    @TableField("start_cabinet_code")
    private String startCabinetCode;

    /**
     * 结束柜号
     */
    @TableField("end_cabinet_code")
    private String endCabinetCode;

    /**
     * 任务状态
     * pending - 待开始
     * in_progress - 进行中
     * completed - 已完成
     * cancelled - 已取消
     */
    @TableField("status")
    private String status;

    /**
     * 开始时间
     */
    @TableField("start_time")
    private LocalDateTime startTime;

    /**
     * 结束时间
     */
    @TableField("end_time")
    private LocalDateTime endTime;

    /**
     * 盘点档案袋总数
     */
    @TableField("total_containers")
    private Integer totalContainers;

    /**
     * 已盘点数量
     */
    @TableField("checked_containers")
    private Integer checkedContainers;

    /**
     * 异常数量
     */
    @TableField("abnormal_containers")
    private Integer abnormalContainers;

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

    /**
     * 更新时间
     */
    @TableField(value = "updated_at", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
