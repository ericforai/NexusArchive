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
