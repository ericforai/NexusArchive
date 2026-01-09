// Input: 复杂度快照数据结构
// Output: HistoryMetadataDto 类
// Pos: DTO 层 - 质量监控
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

package com.nexusarchive.dto.quality;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * 历史元数据 DTO
 *
 * @author Agent D (基础设施工程师)
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HistoryMetadataDto {

    /** 格式版本 */
    private String formatVersion;

    /** 创建时间 */
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", timezone = "UTC")
    private Instant createdAt;

    /** 最后更新时间 */
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", timezone = "UTC")
    private Instant lastUpdated;
}
