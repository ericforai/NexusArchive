// Input: 档案柜实体类
// Output: 档案柜 DTO 类
// Pos: src/main/java/com/nexusarchive/dto/warehouse
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

package com.nexusarchive.dto.warehouse;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import jakarta.validation.constraints.*;

/**
 * 档案柜 DTO
 *
 * 用于创建和更新档案柜的数据传输对象
 *
 * @author Claude Code
 * @since 1.0.0
 */
@Data
@Schema(description = "档案柜创建/更新请求")
public class CabinetDTO {

    @Schema(description = "柜号", example = "C-01")
    @NotBlank(message = "柜号不能为空")
    @Size(max = 50, message = "柜号长度不能超过50个字符")
    private String code;

    @Schema(description = "柜名称", example = "一楼东侧库房")
    @Size(max = 100, message = "柜名称长度不能超过100个字符")
    private String name;

    @Schema(description = "存放位置", example = "三楼东侧库房")
    @Size(max = 200, message = "存放位置长度不能超过200个字符")
    private String location;

    @Schema(description = "层数", example = "5")
    @Min(value = 1, message = "层数至少为1")
    @Max(value = 10, message = "层数不能超过10")
    private Integer rows;

    @Schema(description = "列数", example = "4")
    @Min(value = 1, message = "列数至少为1")
    @Max(value = 10, message = "列数不能超过10")
    private Integer columns;

    @Schema(description = "每列容量", example = "25")
    @Min(value = 1, message = "每列容量至少为1")
    private Integer rowCapacity;

    @Schema(description = "所属全宗ID", example = "1")
    private Long fondsId;

    @Schema(description = "备注", example = "备注信息")
    private String remark;
}
