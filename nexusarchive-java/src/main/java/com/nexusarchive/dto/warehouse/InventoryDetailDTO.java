// Input: 盘点明细请求
// Output: InventoryDetailDTO 请求类
// Pos: src/main/java/com/nexusarchive/dto/warehouse
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

package com.nexusarchive.dto.warehouse;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 盘点明细 DTO
 *
 * 用于记录盘点明细数据
 *
 * @author Claude Code
 * @since 1.0.0
 */
@Data
@Schema(description = "盘点明细请求")
public class InventoryDetailDTO {

    @NotNull
    @Schema(description = "盘点任务ID", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long inventoryId;

    @NotNull
    @Schema(description = "档案袋ID", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long containerId;

    @Schema(description = "预期状态", example = "normal")
    private String expectedStatus;

    @Schema(description = "实际状态", example = "normal")
    private String actualStatus;

    @NotBlank
    @Schema(description = "差异", example = "matched")
    private String difference;

    @Schema(description = "备注")
    @Size(max = 500, message = "备注长度不能超过500个字符")
    private String remark;
}
