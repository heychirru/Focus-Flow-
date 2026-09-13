package com.focusflow.coach.dto;

import java.time.LocalDateTime;

public record InsightsResponse(
        String insights,
        LocalDateTime generatedAt,
        long sessionCount) {
}
