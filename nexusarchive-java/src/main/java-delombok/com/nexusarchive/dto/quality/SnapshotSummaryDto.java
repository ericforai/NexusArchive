// Input: 复杂度快照数据结构
// Output: SnapshotSummaryDto 类
// Pos: DTO 层 - 质量监控
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

package com.nexusarchive.dto.quality;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 快照摘要 DTO
 *
 * @author Agent D (基础设施工程师)
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SnapshotSummaryDto {

    /** 总违规数 */
    private Integer total;

    /** 高严重度违规数 */
    private Integer high;

    /** 中严重度违规数 */
    private Integer medium;

    /** 低严重度违规数 */
    private Integer low;
}
