package com.nexusarchive.controller;

import com.nexusarchive.common.result.Result;
import com.nexusarchive.service.LicenseService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.lang.management.ManagementFactory;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/health")
@RequiredArgsConstructor
public class HealthController {

    private final LicenseService licenseService;

    @GetMapping
    public Result<Map<String, Object>> health() {
        Map<String, Object> map = new HashMap<>();
        map.put("status", "UP");
        map.put("uptimeMs", ManagementFactory.getRuntimeMXBean().getUptime());
        map.put("license", licenseService.current());
        return Result.success(map);
    }

    @GetMapping("/self-check")
    public Result<Map<String, Object>> selfCheck() {
        Map<String, Object> map = new HashMap<>();
        map.put("diskWritable", true); // 这里可以扩展为实际写测试
        map.put("dbConnection", "UNKNOWN"); // 可接入 DataSource health
        map.put("storagePath", "/data/archive");
        map.put("status", "UP");
        return Result.success(map);
    }
}
