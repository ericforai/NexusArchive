// Input: 复杂度快照数据结构
// Output: ComplexityHistoryDto 类
// Pos: DTO 层 - 质量监控
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

package com.nexusarchive.dto.quality;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 完整历史数据 DTO
 *
 * @author Agent D (基础设施工程师)
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ComplexityHistoryDto {

    /** 元数据 */
    private HistoryMetadataDto metadata;

    /** 快照数组 */
    private List<ComplexitySnapshotDto> snapshots;
}
