package com.focusflow.coach.controller;

import com.focusflow.coach.dto.InsightsResponse;
import com.focusflow.coach.service.AiCoachService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/coach")
public class AiCoachController {

    private final AiCoachService aiCoachService;

    public AiCoachController(AiCoachService aiCoachService) {
        this.aiCoachService = aiCoachService;
    }

    @PostMapping("/insights")
    public ResponseEntity<InsightsResponse> generateInsights() {
        return ResponseEntity.ok(aiCoachService.generateInsights());
    }

    @GetMapping("/insights/latest")
    public ResponseEntity<InsightsResponse> getLatestInsights() {
        InsightsResponse latest = aiCoachService.getLatestInsights();
        if (latest == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
        return ResponseEntity.ok(latest);
    }
}
