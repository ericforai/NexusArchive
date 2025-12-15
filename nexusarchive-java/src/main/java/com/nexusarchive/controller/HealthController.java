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
import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/health")
@RequiredArgsConstructor
@Slf4j
public class HealthController {

    private final LicenseService licenseService;
    private final javax.sql.DataSource dataSource;
    private final com.nexusarchive.config.ResilientFlywayRunner flywayRunner;

    @GetMapping
    public Result<Map<String, Object>> health() {
        Map<String, Object> map = new HashMap<>();
        map.put("status", "UP");
        map.put("uptimeMs", ManagementFactory.getRuntimeMXBean().getUptime());
        map.put("license", licenseService.current());
        return Result.success(map);
    }

    @GetMapping("/self-check")
    public org.springframework.http.ResponseEntity<Result<Map<String, Object>>> selfCheck() {
        Map<String, Object> map = new HashMap<>();
        map.put("storagePath", "/data/archive");
        map.put("diskWritable", true); // Optimize later
        
        // 1. Check Migration Status
        String migrationStatus = flywayRunner.getStatus();
        map.put("migration", migrationStatus);
        
        if (!"READY".equals(migrationStatus)) {
             map.put("dbConnection", "UNKNOWN");
             map.put("status", "DOWN");
             return org.springframework.http.ResponseEntity.status(503)
                    .body(Result.error(503, "Service Unavailable: Database Migration in Progress", map));
        }

        // 2. Check DB Connectivity
        boolean dbUp = checkDbStatus();
        map.put("dbConnection", dbUp ? "UP" : "DOWN");
        map.put("status", dbUp ? "UP" : "DOWN");

        if (!dbUp) {
            return org.springframework.http.ResponseEntity.status(503)
                    .body(Result.error(503, "Service Unavailable: Database Down", map));
        }
        
        return org.springframework.http.ResponseEntity.ok(Result.success(map));
    }

    private boolean checkDbStatus() {
        try {
            return java.util.concurrent.CompletableFuture.supplyAsync(() -> {
                try (java.sql.Connection conn = dataSource.getConnection()) {
                    return conn.isValid(1);
                } catch (Exception e) {
                    return false;
                }
            }).get(300, java.util.concurrent.TimeUnit.MILLISECONDS);
        } catch (Exception e) {
            log.warn("Health check failed: {}", e.getMessage());
            return false;
        }
    }
}
