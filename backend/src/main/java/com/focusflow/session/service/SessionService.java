package com.focusflow.session.service;

import com.focusflow.session.dto.CreateTagRequest;
import com.focusflow.session.dto.StatsResponse;
import com.focusflow.session.dto.UpdateTagRequest;
import com.focusflow.session.dto.WeeklyFocusPoint;
import com.focusflow.session.model.Session;
import com.focusflow.session.model.Tag;
import com.focusflow.session.repository.SessionRepository;
import com.focusflow.session.repository.TagRepository;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Date;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Service
public class SessionService {

    private final SessionRepository sessionRepository;
    private final TagRepository tagRepository;

    public SessionService(SessionRepository sessionRepository, TagRepository tagRepository) {
        this.sessionRepository = sessionRepository;
        this.tagRepository = tagRepository;
    }

    @PostConstruct
    public void seedDefaultTags() {
        List<String[]> defaults = List.of(
                new String[]{"General", "#4A90D9"},
                new String[]{"Coding", "#FF6B6B"},
                new String[]{"Writing", "#F7DC6F"},
                new String[]{"Reading", "#82E0AA"},
                new String[]{"Research", "#9B59B6"}
        );
        for (String[] entry : defaults) {
            if (!tagRepository.existsByName(entry[0])) {
                tagRepository.save(new Tag(null, entry[0], entry[1]));
            }
        }
    }

    @Transactional
    public Session start(String tag, int durationMinutes) {
        if (durationMinutes <= 0) {
            throw new IllegalArgumentException("durationMinutes must be greater than 0");
        }

        String resolvedTag = tag == null || tag.isBlank() ? "General" : tag.trim();
        if (!tagRepository.existsByName(resolvedTag)) {
            throw new IllegalArgumentException("Unknown tag '" + resolvedTag + "'");
        }

        Session session = new Session();
        session.setTag(resolvedTag);
        session.setDurationMinutes(durationMinutes);
        session.setStartTime(LocalDateTime.now());
        session.setCompleted(false);
        return sessionRepository.save(session);
    }

    @Transactional
    public Session complete(Long id) {
        Session session = findById(id);
        session.setEndTime(LocalDateTime.now());
        session.setCompleted(true);
        return sessionRepository.save(session);
    }

    @Transactional
    public Session cancel(Long id) {
        Session session = findById(id);
        session.setEndTime(LocalDateTime.now());
        session.setCompleted(false);
        return sessionRepository.save(session);
    }

    public Session findById(Long id) {
        return sessionRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Session with id " + id + " not found"));
    }

    public List<Session> findAll() {
        return sessionRepository.findAllByOrderByStartTimeDesc();
    }

    public List<Session> findByTag(String tag) {
        return sessionRepository.findByTagOrderByStartTimeDesc(tag);
    }

    public List<Session> findSessions(int limit, int offset, String tag) {
        if (limit <= 0) {
            throw new IllegalArgumentException("limit must be greater than 0");
        }
        if (offset < 0) {
            throw new IllegalArgumentException("offset must be 0 or greater");
        }

        List<Session> source = tag != null && !tag.isBlank()
                ? findByTag(tag.trim())
                : findAll();

        if (offset >= source.size()) {
            return List.of();
        }

        int end = Math.min(offset + limit, source.size());
        return source.subList(offset, end);
    }

    public StatsResponse buildStats() {
        long total = sessionRepository.count();
        long completed = sessionRepository.countByCompleted(true);
        long totalMinutes = sessionRepository.findAllByOrderByStartTimeDesc().stream()
                .filter(Session::isCompleted)
                .mapToLong(Session::getDurationMinutes)
                .sum();

        LocalDateTime startOfDay = LocalDate.now().atStartOfDay();
        LocalDateTime endOfDay = startOfDay.plusDays(1);
        long todayMinutes = sessionRepository.findTodaySessions(startOfDay, endOfDay).stream()
                .filter(Session::isCompleted)
                .mapToLong(Session::getDurationMinutes)
                .sum();

        double completionRate = total == 0 ? 0.0 : (double) completed / total;
        int streak = calculateStreak();

        List<StatsResponse.TagStat> topTags = sessionRepository.findTagStats().stream()
                .limit(5)
                .map(row -> new StatsResponse.TagStat(
                        (String) row[0],
                        ((Number) row[1]).longValue(),
                        ((Number) row[2]).longValue()))
                .collect(Collectors.toList());

        return new StatsResponse(total, totalMinutes, todayMinutes, completionRate, streak, topTags);
    }

    private int calculateStreak() {
        List<Date> days = sessionRepository.findDistinctCompletedDays();
        if (days.isEmpty()) {
            return 0;
        }

        List<LocalDate> dates = days.stream()
                .map(Date::toLocalDate)
                .sorted((left, right) -> right.compareTo(left))
                .toList();

        LocalDate today = LocalDate.now();
        LocalDate yesterday = today.minusDays(1);
        if (!dates.get(0).equals(today) && !dates.get(0).equals(yesterday)) {
            return 0;
        }

        int streak = 1;
        for (int i = 1; i < dates.size(); i++) {
            if (dates.get(i - 1).minusDays(1).equals(dates.get(i))) {
                streak++;
            } else {
                break;
            }
        }
        return streak;
    }

    public String buildTextSummary() {
        StatsResponse stats = buildStats();
        StringBuilder summary = new StringBuilder();
        summary.append("Total completed sessions: ").append(stats.totalSessions()).append("\n");
        summary.append("Total focus minutes logged: ").append(stats.totalMinutes()).append("\n");
        summary.append("Today's focus minutes: ").append(stats.todayMinutes()).append("\n");
        summary.append("Current streak: ").append(stats.currentStreak()).append(" days\n");
        summary.append("Session completion rate: ")
                .append(String.format("%.0f%%", stats.completionRate() * 100)).append("\n\n");

        summary.append("Session breakdown by category:\n");
        stats.topTags().forEach(tag -> summary.append("- ").append(tag.tag()).append(": ")
                .append(tag.sessions()).append(" sessions, ")
                .append(tag.totalMinutes()).append(" min total\n"));

        summary.append("\nLast 7 days activity:\n");
        LocalDateTime now = LocalDateTime.now();
        for (int i = 0; i < 7; i++) {
            LocalDateTime start = now.minusDays(i).toLocalDate().atStartOfDay();
            LocalDateTime end = start.plusDays(1);
            List<Session> daySessions = sessionRepository.findTodaySessions(start, end);
            long minutes = daySessions.stream()
                    .filter(Session::isCompleted)
                    .mapToLong(Session::getDurationMinutes)
                    .sum();
            summary.append("- ").append(start.toLocalDate().format(DateTimeFormatter.ISO_LOCAL_DATE))
                    .append(": ").append(daySessions.size()).append(" sessions, ").append(minutes).append(" min\n");
        }

        return summary.toString();
    }

    public List<WeeklyFocusPoint> buildWeeklyFocus(int days) {
        if (days <= 0 || days > 31) {
            throw new IllegalArgumentException("days must be between 1 and 31");
        }

        LocalDate today = LocalDate.now();
        return IntStream.range(0, days)
                .mapToObj(index -> today.minusDays(days - 1L - index))
                .map(day -> {
                    LocalDateTime start = day.atStartOfDay();
                    LocalDateTime end = start.plusDays(1);
                    List<Session> completedSessions = sessionRepository.findCompletedSessionsBetween(start, end);
                    long minutes = completedSessions.stream()
                            .mapToLong(Session::getDurationMinutes)
                            .sum();
                    return new WeeklyFocusPoint(
                            day.format(DateTimeFormatter.ISO_LOCAL_DATE),
                            completedSessions.size(),
                            minutes
                    );
                })
                .toList();
    }

    public String exportSessionsCsv(String tag) {
        List<Session> sessions = tag != null && !tag.isBlank()
                ? findByTag(tag.trim())
                : findAll();

        StringBuilder csv = new StringBuilder();
        csv.append("id,tag,durationMinutes,startTime,endTime,completed\n");
        sessions.forEach(session -> csv.append(csvEscape(session.getId()))
                .append(",").append(csvEscape(session.getTag()))
                .append(",").append(csvEscape(session.getDurationMinutes()))
                .append(",").append(csvEscape(session.getStartTime()))
                .append(",").append(csvEscape(session.getEndTime()))
                .append(",").append(csvEscape(session.isCompleted()))
                .append("\n"));
        return csv.toString();
    }

    public List<Tag> findAllTags() {
        return tagRepository.findAll().stream()
                .sorted((left, right) -> left.getName().compareToIgnoreCase(right.getName()))
                .toList();
    }

    @Transactional
    public Tag createTag(CreateTagRequest req) {
        String name = normalizeTagName(req.name());
        if (tagRepository.existsByName(name)) {
            throw new IllegalArgumentException("Tag '" + name + "' already exists");
        }

        return tagRepository.save(new Tag(null, name, normalizeTagColor(req.color())));
    }

    @Transactional
    public Tag updateTag(Long id, UpdateTagRequest req) {
        Tag tag = tagRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Tag with id " + id + " not found"));

        String nextName = normalizeTagName(req.name());
        tagRepository.findByName(nextName)
                .filter(existing -> !existing.getId().equals(id))
                .ifPresent(existing -> {
                    throw new IllegalArgumentException("Tag '" + nextName + "' already exists");
                });

        String previousName = tag.getName();
        tag.setName(nextName);
        tag.setColor(normalizeTagColor(req.color()));
        Tag savedTag = tagRepository.save(tag);

        if (!previousName.equals(nextName)) {
            sessionRepository.findByTagOrderByStartTimeDesc(previousName)
                    .forEach(session -> session.setTag(nextName));
        }

        return savedTag;
    }

    private String normalizeTagName(String name) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Tag name is required");
        }
        return name.trim();
    }

    private String normalizeTagColor(String color) {
        String resolved = color == null || color.isBlank() ? "#4A90D9" : color.trim();
        if (!resolved.matches("^#[0-9A-Fa-f]{6}$")) {
            throw new IllegalArgumentException("Tag color must be a hex value like #4A90D9");
        }
        return resolved.toUpperCase();
    }

    private String csvEscape(Object value) {
        if (value == null) {
            return "";
        }
        String raw = String.valueOf(value);
        if (raw.contains(",") || raw.contains("\"") || raw.contains("\n")) {
            return "\"" + raw.replace("\"", "\"\"") + "\"";
        }
        return raw;
    }
}
