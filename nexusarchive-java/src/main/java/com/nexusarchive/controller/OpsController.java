package com.nexusarchive.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/ops")
public class OpsController {
    @GetMapping("/self-check")
    public ResponseEntity<String> selfCheck() {
        return ResponseEntity.ok("OK");
    }
}
