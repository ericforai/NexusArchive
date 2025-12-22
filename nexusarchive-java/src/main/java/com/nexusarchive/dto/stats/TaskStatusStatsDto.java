// Input: Lombok、Java 标准库
// Output: TaskStatusStatsDto 类
// Pos: 数据传输对象
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

package com.nexusarchive.dto.stats;

import lombok.Builder;
import lombok.Data;

import java.util.Map;

@Data
@Builder
public class TaskStatusStatsDto {
    private long total;
    private long completed;
    private long failed;
    private long running;
    private long pending;
    private Map<String, Long> byStatus;
}
