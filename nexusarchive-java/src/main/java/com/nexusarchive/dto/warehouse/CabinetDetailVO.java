// Input: ArchiveCabinet 实体
// Output: CabinetDetailVO 响应类
// Pos: src/main/java/com/nexusarchive/dto/warehouse
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

package com.nexusarchive.dto.warehouse;

import com.nexusarchive.entity.warehouse.ArchiveCabinet;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

/**
 * 档案柜详情 VO
 *
 * 包含档案柜的完整信息，包括：
 * - 基本信息（柜号、名称、位置）
 * - 容量信息（层数、列数、总容量）
 * - 使用情况（当前数量、使用率）
 * - 所属档案袋列表（统计信息）
 *
 * @author Claude Code
 * @since 1.0.0
 */
@Data
@Schema(description = "档案柜详情响应")
public class CabinetDetailVO {

    @Schema(description = "档案柜ID")
    private Long id;

    @Schema(description = "柜号")
    private String code;

    @Schema(description = "柜名称")
    private String name;

    @Schema(description = "存放位置")
    private String location;

    @Schema(description = "层数")
    private Integer rows;

    @Schema(description = "列数")
    private Integer columns;

    @Schema(description = "每列容量")
    private Integer rowCapacity;

    @Schema(description = "总容量")
    private Integer totalCapacity;

    @Schema(description = "当前档案袋数量")
    private Integer currentCount;

    @Schema(description = "使用率百分比")
    private Integer usageRate;

    @Schema(description = "状态")
    private String status;

    @Schema(description = "所属全宗ID")
    private Long fondsId;

    @Schema(description = "备注")
    private String remark;

    @Schema(description = "创建时间")
    private String createdAt;

    @Schema(description = "更新时间")
    private String updatedAt;

    @Schema(description = "档案袋列表")
    private List<ContainerSummaryVO> containers;

    @Schema(description = "容量统计")
    private CapacitySummaryVO capacity;

    /**
     * 档案柜摘要 VO
     */
    @Data
    @Schema(description = "档案柜摘要")
    public static class CabinetSummaryVO {
        @Schema(description = "档案柜ID")
        private Long id;

        @Schema(description = "柜号")
        private String code;

        @Schema(description = "柜名称")
        private String name;

        @Schema(description = "存放位置")
        private String location;
    }

    @Schema(description = "状态分布")
    private List<StatusDistributionVO> statusDistribution;

    /**
     * 从实体转换为 VO
     *
     * @param entity 档案柜实体
     * @return 详情 VO
     */
    public static CabinetDetailVO fromEntity(ArchiveCabinet entity) {
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

    /**
     * 档案袋摘要 VO
     */
    @Data
    @Schema(description = "档案袋摘要")
    public static class ContainerSummaryVO {
        @Schema(description = "档案袋ID")
        private Long id;

        @Schema(description = "袋号")
        private String containerNo;

        @Schema(description = "柜内位置")
        private String cabinetPosition;

        @Schema(description = "状态")
        private String status;

        @Schema(description = "使用率")
        private Integer usageRate;
    }

    /**
     * 容量统计 VO
     */
    @Data
    @Schema(description = "容量统计")
    public static class CapacitySummaryVO {
        @Schema(description = "总容量")
        private Integer totalCapacity;

        @Schema(description = "已使用")
        private Integer usedCapacity;

        @Schema(description = "剩余容量")
        private Integer remainingCapacity;

        @Schema(description = "使用率百分比")
        private Integer usageRate;
    }

    /**
     * 状态分布 VO
     */
    @Data
    @Schema(description = "状态分布")
    public static class StatusDistributionVO {
        @Schema(description = "状态值")
        private String status;

        @Schema(description = "数量")
        private Integer count;

        @Schema(description = "百分比")
        private Integer percentage;
    }
}
