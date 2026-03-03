// Input: 盘点完成请求
// Output: InventoryCompleteDTO 请求类
// Pos: src/main/java/com/nexusarchive/dto/warehouse
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

package com.nexusarchive.dto.warehouse;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 盘点完成 DTO
 *
 * 用于完成盘点任务时提交盘点结果
 *
 * @author Claude Code
 * @since 1.0.0
 */
@Data
@Schema(description = "盘点完成请求")
public class InventoryCompleteDTO {

    @NotNull
    @Schema(description = "档案袋总数", requiredMode = Schema.RequiredMode.REQUIRED, example = "100")
    private Integer totalContainers;

    @NotNull
    @Schema(description = "已盘点数", requiredMode = Schema.RequiredMode.REQUIRED, example = "98")
    @Min(value = 0, message = "已盘点数不能为负数")
    private Integer checkedContainers;

    @NotNull
    @Schema(description = "异常数（损坏+遗失+多余）", requiredMode = Schema.RequiredMode.REQUIRED, example = "2")
    @Min(value = 0, message = "异常数不能为负数")
    private Integer abnormalContainers;

    /**
     * 验证数据一致性
     *
     * @return 是否一致
     */
    public boolean isValid() {
        return checkedContainers >= 0
            && abnormalContainers >= 0
            && (checkedContainers + abnormalContainers) == totalContainers;
    }
}
