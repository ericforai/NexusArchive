// Input: Java 标准库、本地模块
// Output: NotificationService 接口
// Pos: 业务服务层
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

package com.nexusarchive.service;

import com.nexusarchive.dto.notification.NotificationDto;

import java.util.List;

public interface NotificationService {
    List<NotificationDto> listLatest();
}
