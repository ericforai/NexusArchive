// Input: ArchiveContainer 实体类
// Output: 档案袋 DTO 和 VO 类
// Pos: src/main/java/com/nexusarchive/dto/warehouse
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

package com.nexusarchive.dto.warehouse;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import jakarta.validation.constraints.*;
import java.util.List;

/**
 * 档案袋 DTO
 *
 * 用于创建和更新档案袋的数据传输对象
 *
 * @author Claude Code
 * @since 1.0.0
 */
@Data
@Schema(description = "档案袋创建/更新请求")
public class ContainerDTO {

    @Schema(description = "袋号", example = "CN-2024-001")
    @Size(max = 50, message = "袋号长度不能超过50个字符")
    private String containerNo;

    @Schema(description = "所属档案柜ID", example = "1")
    private Long cabinetId;

    @Schema(description = "柜内位置描述", example = "第3层第2列")
    @Size(max = 100, message = "柜内位置长度不能超过100个字符")
    private String cabinetPosition;

    @Schema(description = "物理定位", example = "RFID:ABC123")
    @Size(max = 200, message = "物理定位长度不能超过200个字符")
    private String physicalLocation;

    @Schema(description = "关联的案卷ID列表")
    private List<Long> volumeIds;

    @Schema(description = "袋容量（盒数）", example = "50")
    @Min(value = 1, message = "容量必须大于0")
    private Integer capacity;

    @Schema(description = "所属全宗ID", example = "1")
    private Long fondsId;

    @Schema(description = "是否为主卷", example = "false")
    private Boolean isPrimary;

    @Schema(description = "状态", example = "empty")
    @Size(max = 20, message = "状态长度不能超过20个字符")
    private String status;

    @Schema(description = "备注", example = "备注信息")
    @Size(max = 500, message = "备注长度不能超过500个字符")
    private String remark;
}
