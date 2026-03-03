// Input: ArchiveContainer 实体类
// Output: ContainerDetailVO 响应类
// Pos: src/main/java/com/nexusarchive/dto/warehouse
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

package com.nexusarchive.dto.warehouse;

import com.nexusarchive.entity.warehouse.ArchiveContainer;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 档案袋详情 VO
 *
 * 包含档案袋的完整信息，包括：
 * - 基本信息（袋号、柜号、位置）
 * - 容量信息（容量、已装盒数、使用率）
 * - 关联案卷列表
 * - 盘点信息
 *
 * @author Claude Code
 * @since 1.0.0
 */
@Data
@Schema(description = "档案袋详情响应")
public class ContainerDetailVO {

    @Schema(description = "档案袋ID", example = "1001")
    private Long id;

    @Schema(description = "袋号", example = "CN-2024-001")
    private String containerNo;

    @Schema(description = "所属档案柜ID", example = "10")
    private Long cabinetId;

    @Schema(description = "柜号", example = "C-01")
    private String cabinetCode;

    @Schema(description = "柜内位置", example = "第3层第2列")
    private String cabinetPosition;

    @Schema(description = "物理定位", example = "RFID:ABC123")
    private String physicalLocation;

    @Schema(description = "关联的案卷ID")
    private Long volumeId;

    @Schema(description = "袋容量（盒数）", example = "50")
    private Integer capacity;

    @Schema(description = "已装盒档案数量", example = "25")
    private Integer archiveCount;

    @Schema(description = "使用率", example = "50")
    private Integer usageRate;

    @Schema(description = "档案袋状态", example = "normal")
    private String status;

    @Schema(description = "盘点状态", example = "checked")
    private String checkStatus;

    @Schema(description = "最近盘点任务ID")
    private Long lastInventoryId;

    @Schema(description = "最近盘点时间", example = "2024-01-13 10:00:00")
    private String lastInventoryTime;

    @Schema(description = "最近盘点结果", example = "normal")
    private String lastInventoryResult;

    @Schema(description = "所属全宗ID")
    private Long fondsId;

    @Schema(description = "备注")
    private String remark;

    @Schema(description = "创建时间", example = "2024-01-13 10:00:00")
    private String createdAt;

    @Schema(description = "更新时间", example = "2024-01-13 10:00:00")
    private String updatedAt;

    @Schema(description = "档案柜信息")
    private CabinetSummaryVO cabinet;

    @Schema(description = "关联案卷列表")
    private List<VolumeSummaryVO> volumes;

    @Schema(description = "容量统计")
    private CapacitySummaryVO capacityInfo;

    /**
     * 从实体转换为 VO
     *
     * @param entity 档案袋实体
     * @return 详情 VO
     */
    public static ContainerDetailVO fromEntity(ArchiveContainer entity) {
        if (entity == null) {
            return null;
        }

        ContainerDetailVO vo = new ContainerDetailVO();
        vo.setId(entity.getId());
        vo.setContainerNo(entity.getContainerNo());
        vo.setCabinetId(entity.getCabinetId());
        vo.setCabinetPosition(entity.getCabinetPosition());
        vo.setPhysicalLocation(entity.getPhysicalLocation());
        vo.setVolumeId(entity.getVolumeId());
        vo.setCapacity(entity.getCapacity());
        vo.setArchiveCount(entity.getArchiveCount());
        vo.setStatus(entity.getStatus());
        vo.setCheckStatus(entity.getCheckStatus());
        vo.setFondsId(entity.getFondsId());
        vo.setRemark(entity.getRemark());
        vo.setCreatedAt(entity.getCreatedAt().toString());
        vo.setUpdatedAt(entity.getUpdatedAt().toString());

        // 计算使用率
        if (entity.getCapacity() != null && entity.getCapacity() > 0) {
            vo.setUsageRate(entity.getArchiveCount() * 100 / entity.getCapacity());
        }

        return vo;
    }

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

    /**
     * 案卷摘要 VO
     */
    @Data
    @Schema(description = "案卷摘要")
    public static class VolumeSummaryVO {
        @Schema(description = "案卷ID")
        private Long id;

        @Schema(description = "案卷号")
        private String volumeCode;

        @Schema(description = "年度")
        private String year;

        @Schema(description = "保管期限")
        private String retentionPeriod;

        @Schema(description = "状态")
        private String status;
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
}
