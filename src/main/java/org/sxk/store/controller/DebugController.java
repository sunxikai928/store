package org.sxk.store.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/debug")
public class DebugController {

    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> healthCheck() {
        log.info("Health check requested");
        Map<String, Object> response = new HashMap<>();
        response.put("status", "UP");
        response.put("service", "store");
        response.put("timestamp", System.currentTimeMillis());
        return ResponseEntity.ok(response);
    }

    @GetMapping("/env")
    public ResponseEntity<Map<String, String>> getEnvironment() {
        Map<String, String> env = new HashMap<>();
        env.put("java.version", System.getProperty("java.version"));
        env.put("os.name", System.getProperty("os.name"));
        env.put("user.dir", System.getProperty("user.dir"));
        return ResponseEntity.ok(env);
    }

    @GetMapping("/test-error")
    public ResponseEntity<String> testError() {
        throw new RuntimeException("Test error for debugging");
    }
}