// Input: Lombok、Java 标准库
// Output: StorageStatsDto 类
// Pos: 数据传输对象
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

package com.nexusarchive.dto.stats;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class StorageStatsDto {
    private String total;
    private String used;
    private double usagePercent;
}
