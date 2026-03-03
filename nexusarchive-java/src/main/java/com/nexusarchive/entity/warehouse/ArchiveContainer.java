// Input: MyBatis-Plus 注解、Lombok、JPA 规范
// Output: 档案袋实体类
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
 * 档案袋实体类
 *
 * 核心职责：
 * 1. 管理档案袋的基本信息（袋号、柜号、位置）
 * 2. 关联档案柜和电子案卷（一对多）
 * 3. 支持实物定位（RFID/二维码）
 * 4. 状态管理（空袋/正常/满袋/已损坏/借出/待盘点）
 * 5. 盘点信息记录
 *
 * @author Claude Code
 * @since 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("archives_container")
public class ArchiveContainer implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 主键ID
     */
    @TableId(value = "id", type = IdType.ASSIGN_ID)
    private Long id;

    /**
     * 袋号
     * 格式：CN-{YYYY}-{4位流水号}
     * 示例：CN-2024-0001
     */
    @TableField("container_no")
    private String containerNo;

    /**
     * 所属档案柜ID
     */
    @TableField("cabinet_id")
    private Long cabinetId;

    /**
     * 柜内位置描述
     * 格式：第3层第2列
     * 示例：第3层第2列
     */
    @TableField("cabinet_position")
    private String cabinetPosition;

    /**
     * 物理定位
     * 支持 RFID 标签或二维码
     */
    @TableField("physical_location")
    private String physicalLocation;

    /**
     * 关联的电子案卷ID
     * 可为空（仅实物未关联电子档案时）
     */
    @TableField("volume_id")
    private Long volumeId;

    /**
     * 袋容量（盒数）
     */
    @TableField("capacity")
    private Integer capacity;

    /**
     * 已装盒档案数量
     */
    @TableField("archive_count")
    private Integer archiveCount;

    /**
     * 档案袋状态
     * empty - 空袋
     * normal - 正常（已有关联案卷）
     * full - 满袋
     * damaged - 已损坏
     * borrowed - 借出中
     * pending - 待盘点
     */
    @TableField("status")
    private String status;

    /**
     * 盘点状态
     * pending - 待盘点
     * checked - 已盘点
     * unchecked - 未盘点
     */
    @TableField("check_status")
    private String checkStatus;

    /**
     * 最近盘点任务ID
     */
    @TableField("last_inventory_id")
    private Long lastInventoryId;

    /**
     * 最近盘点时间
     */
    @TableField("last_inventory_time")
    private LocalDateTime lastInventoryTime;

    /**
     * 最近盘点结果
     * normal - 正常
     * damaged - 已损坏
     * missing - 遗失
     * extra - 多余
     */
    @TableField("last_inventory_result")
    private String lastInventoryResult;

    /**
     * 所属全宗ID
     */
    @TableField("fonds_id")
    private Long fondsId;

    /**
     * 备注
     */
    @TableField("remark")
    private String remark;

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

    /**
     * 租户ID（多租户）
     */
    @TableField("tenant_id")
    private String tenantId;

    /**
     * 是否删除
     */
    @TableLogic
    private Boolean deleted = false;
}
