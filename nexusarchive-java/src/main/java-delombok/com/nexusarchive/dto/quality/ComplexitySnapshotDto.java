// Input: 复杂度快照数据结构
// Output: ComplexitySnapshotDto 类
// Pos: DTO 层 - 质量监控
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

package com.nexusarchive.dto.quality;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;

/**
 * 复杂度快照 DTO
 *
 * @author Agent D (基础设施工程师)
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ComplexitySnapshotDto {

    /** 快照时间戳 */
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", timezone = "UTC")
    private Instant timestamp;

    /** Git commit hash */
    private String commit;

    /** Git 分支名 */
    private String branch;

    /** 违规摘要 */
    private SnapshotSummaryDto summary;

    /** 违规文件列表 */
    private List<FileViolationDto> files;
}
