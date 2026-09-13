package com.focusflow.session.dto;

import java.util.List;

public record StatsResponse(
        long totalSessions,
        long totalMinutes,
        long todayMinutes,
        double completionRate,
        int currentStreak,
        List<TagStat> topTags
) {
    public record TagStat(String tag, long sessions, long totalMinutes) {}
}
