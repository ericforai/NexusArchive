// Input: MyBatis-Plus 注解、Lombok、JPA 规范
// Output: 档案袋-案卷关联实体类
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
 * 档案袋-案卷关联实体类
 *
 * 核心职责：
 * 1. 关联档案袋和电子案卷（一对多）
 * 2. 支持主卷标记（用于排序显示）
 * 3. 记录装盒时间和操作人
 * 4. 联级删除支持
 *
 * @author Claude Code
 * @since 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("archives_container_volume")
public class ArchiveContainerVolume implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 主键ID
     */
    @TableId(value = "id", type = IdType.ASSIGN_ID)
    private Long id;

    /**
     * 档案袋ID
     */
    @TableField("container_id")
    private Long containerId;

    /**
     * 案卷ID
     */
    @TableField("volume_id")
    private Long volumeId;

    /**
     * 是否主卷
     * 用于排序和显示
     */
    @TableField("is_primary")
    private Boolean isPrimary;

    /**
     * 显示顺序
     */
    @TableField("display_order")
    private Integer displayOrder;

    /**
     * 装盒时间
     */
    @TableField("boxed_at")
    private LocalDateTime boxedAt;

    /**
     * 装盒操作人ID
     */
    @TableField("boxed_by")
    private Long boxedBy;

    /**
     * 创建时间
     */
    @TableField(value = "created_at", fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
