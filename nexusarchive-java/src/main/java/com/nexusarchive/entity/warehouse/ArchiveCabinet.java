// Input: MyBatis-Plus 注解、Lombok、JPA 规范
// Output: 档案柜实体类
// Pos: src/main/java/com/nexusarchive/entity/warehouse
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

package com.nexusarchive.entity.warehouse;

import com.baomidou.mybatisplus.annotation.*;
import com.nexusarchive.dto.warehouse.CabinetDetailVO;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.io.Serial;
import java.time.LocalDateTime;

/**
 * 档案柜实体类
 *
 * 核心职责：
 * 1. 管理物理档案柜的基本信息
 * 2. 层、列、容量计算
 * 3. 当前档案袋数量统计
 * 4. 使用率自动计算
 *
 * @author Claude Code
 * @since 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("archives_cabinet")
public class ArchiveCabinet implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 主键ID
     */
    @TableId(value = "id", type = IdType.ASSIGN_ID)
    private Long id;

    /**
     * 柜号
     * 格式：C-01, C-02, ...
     * 规则：按全宗内顺序编号
     */
    @TableField("code")
    private String code;

    /**
     * 柜名称
     * 示例：一楼东侧库房、三楼密集架室
     */
    @TableField("name")
    private String name;

    /**
     * 存放位置
     */
    @TableField("location")
    private String location;

    /**
     * 层数
     * 默认：5层
     */
    @TableField("rows")
    private Integer rows;

    /**
     * 列数
     * 默认：4列
     */
    @TableField("columns")
    private Integer columns;

    /**
     * 每列容量（档案袋数）
     * 默认：25
     */
    @TableField("row_capacity")
    private Integer rowCapacity;

    /**
     * 总容量（计算列）
     * rows * columns * rowCapacity
     */
    @TableField(value = "total_capacity", insertStrategy = FieldStrategy.ALWAYS)
    private Integer totalCapacity;

    /**
     * 当前档案袋数量
     */
    @TableField("current_count")
    private Integer currentCount;

    /**
     * 使用率百分比
     * 计算值：current_count / total_capacity * 100
     */
    @TableField(exist = false)
    private Integer usageRate;

    /**
     * 状态
     * normal - 正常使用
     * disabled - 已停用
     * full - 已满
     */
    @TableField("status")
    private String status;

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
     * 转换为 VO
     *
     * @param entity 档案柜实体
     * @return 详情 VO
     */
    public static CabinetDetailVO toVO(ArchiveCabinet entity) {
        if (entity == null) {
            return null;
        }

        CabinetDetailVO vo = new CabinetDetailVO();
        vo.setId(entity.getId());
        vo.setCode(entity.getCode());
        vo.setName(entity.getName());
        vo.setLocation(entity.getLocation());
        vo.setRows(entity.getRows());
        vo.setColumns(entity.getColumns());
        vo.setRowCapacity(entity.getRowCapacity());
        vo.setTotalCapacity(entity.getTotalCapacity());
        vo.setCurrentCount(entity.getCurrentCount());

        // 计算使用率
        if (entity.getTotalCapacity() != null && entity.getTotalCapacity() > 0) {
            vo.setUsageRate(entity.getCurrentCount() * 100 / entity.getTotalCapacity());
        }

        vo.setStatus(entity.getStatus());
        vo.setFondsId(entity.getFondsId());
        vo.setRemark(entity.getRemark());
        vo.setCreatedAt(entity.getCreatedAt().toString());
        vo.setUpdatedAt(entity.getUpdatedAt().toString());

        return vo;
    }
}
