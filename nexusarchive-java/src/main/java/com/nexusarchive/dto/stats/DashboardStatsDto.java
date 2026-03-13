// Input: Lombok、Java 标准库
// Output: DashboardStatsDto 类
// Pos: 数据传输对象
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

package com.nexusarchive.dto.stats;

import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DashboardStatsDto {
    private long totalArchives;
    private String storageUsed;
    private long pendingTasks;
    private long todayIngest;
    private List<ArchivalTrendDto> recentTrend;
}
