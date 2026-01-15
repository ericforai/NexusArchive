// Input: 复杂度快照数据结构
// Output: FileViolationDto 类
// Pos: DTO 层 - 质量监控
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

package com.nexusarchive.dto.quality;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 文件违规详情 DTO
 *
 * @author Agent D (基础设施工程师)
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FileViolationDto {

    /** 文件路径（相对于项目根目录） */
    private String path;

    /** 文件总行数 */
    private Integer lines;

    /** 最大函数行数 */
    private Integer maxFunctionLines;

    /** 圈复杂度 */
    private Integer complexity;

    /** 违规规则列表 */
    private List<String> violations;
}
