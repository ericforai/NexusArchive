// Input: Lombok、Java 标准库
// Output: NotificationDto 类
// Pos: 数据传输对象
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

package com.nexusarchive.dto.notification;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class NotificationDto {
    private String id;
    private String title;
    private String time;
    private String type; // info / warning / success
}
