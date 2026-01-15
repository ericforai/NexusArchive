// Input: Lombok、Java 标准库
// Output: WorkflowTaskDto 类
// Pos: 数据传输对象
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

package com.nexusarchive.dto.workflow;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class WorkflowTaskDto {
    private String id;
    private String name;
    private String assignee;
    private String status;
    private String createdTime;
    private String businessKey;
    private String businessType; // e.g., BORROWING, DESTRUCTION
}
