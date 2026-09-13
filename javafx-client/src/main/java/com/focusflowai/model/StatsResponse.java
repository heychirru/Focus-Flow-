package com.focusflowai.model;

import java.util.List;

public class StatsResponse {
    public long totalSessions;
    public long totalMinutes;
    public long todayMinutes;
    public double completionRate;
    public int currentStreak;
    public List<TagStat> topTags;

    public static class TagStat {
        public String tag;
        public long sessions;
        public long totalMinutes;
    }
}
