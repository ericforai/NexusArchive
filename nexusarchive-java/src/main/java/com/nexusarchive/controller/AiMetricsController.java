// nexusarchive-java/src/main/java/com/nexusarchive/controller/AiMetricsController.java
package com.nexusarchive.controller;

import com.nexusarchive.integration.erp.ai.monitoring.AiGenerationMetrics;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/ai")
public class AiMetricsController {

    @Autowired
    private AiGenerationMetrics metrics;

    @GetMapping("/metrics")
    public ResponseEntity<?> getMetrics() {
        return ResponseEntity.ok(metrics.getSnapshot());
    }
}
