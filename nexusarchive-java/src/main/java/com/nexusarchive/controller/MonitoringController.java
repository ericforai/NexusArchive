// Input: Lombok、Spring Framework、Java 标准库、本地模块
// Output: MonitoringController 类
// Pos: 接口层 Controller
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

package com.nexusarchive.controller;

import com.nexusarchive.common.result.Result;
import com.nexusarchive.dto.monitoring.IntegrationMonitoringDTO;
import com.nexusarchive.service.MonitoringService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/monitoring")
@RequiredArgsConstructor
public class MonitoringController {

    private final MonitoringService monitoringService;

    @GetMapping("/integration")
    public Result<IntegrationMonitoringDTO> getIntegrationMetrics() {
        return Result.success(monitoringService.getIntegrationMetrics());
    }
}
