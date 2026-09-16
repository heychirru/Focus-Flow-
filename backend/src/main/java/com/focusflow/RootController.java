package com.focusflow;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
public class RootController {

    @GetMapping("/")
    public Map<String, Object> root() {
        return Map.of(
                "name", "FocusFlow",
                "status", "running",
                "version", "2.0.0",
                "api", "/api",
                "health", "/actuator/health"
        );
    }
}
