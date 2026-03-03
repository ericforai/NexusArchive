// Input: 盘点业务需求
// Output: InventoryDTO 请求类
// Pos: src/main/java/com/nexusarchive/dto/warehouse
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

package com.nexusarchive.dto.warehouse;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 盘点任务 DTO
 *
 * 用于创建盘点任务的数据传输对象
 *
 * @author Claude Code
 * @since 1.0.0
 */
@Data
@Schema(description = "盘点任务创建/更新请求")
public class InventoryDTO {

    @Schema(description = "任务名称", example = "2024年第一季度盘点")
    @NotBlank(message = "任务名称不能为空")
    @Size(max = 100, message = "任务名称长度不能超过100个字符")
    private String taskName;

    @Schema(description = "盘点档案柜ID", example = "1")
    private Long cabinetId;

    @Schema(description = "起始柜号", example = "C-01")
    @Size(max = 50, message = "起始柜号长度不能超过50个字符")
    private String startCabinetCode;

    @Schema(description = "结束柜号", example = "C-10")
    @Size(max = 50, message = "结束柜号长度不能超过50个字符")
    private String endCabinetCode;

    @Schema(description = "所属全宗ID", example = "1")
    private Long fondsId;

    @Schema(description = "备注", example = "备注信息")
    @Size(max = 500, message = "备注长度不能超过500个字符")
    private String remark;
}
