package com.focusflow.session.controller;

import com.focusflow.session.dto.CreateSessionRequest;
import com.focusflow.session.dto.StatsResponse;
import com.focusflow.session.dto.WeeklyFocusPoint;
import com.focusflow.session.model.Session;
import com.focusflow.session.service.SessionService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/sessions")
public class SessionController {

    private final SessionService sessionService;

    public SessionController(SessionService sessionService) {
        this.sessionService = sessionService;
    }

    @PostMapping
    public ResponseEntity<Session> create(@RequestBody CreateSessionRequest req) {
        Session session = sessionService.start(req.tag(), req.durationMinutes());
        return ResponseEntity.status(HttpStatus.CREATED).body(session);
    }

    @PutMapping("/{id}/complete")
    public ResponseEntity<Session> complete(@PathVariable Long id) {
        return ResponseEntity.ok(sessionService.complete(id));
    }

    @PutMapping("/{id}/cancel")
    public ResponseEntity<Session> cancel(@PathVariable Long id) {
        return ResponseEntity.ok(sessionService.cancel(id));
    }

    @GetMapping
    public ResponseEntity<List<Session>> getAll(@RequestParam(defaultValue = "100") int limit,
                                                @RequestParam(defaultValue = "0") int offset,
                                                @RequestParam(required = false) String tag) {
        return ResponseEntity.ok(sessionService.findSessions(limit, offset, tag));
    }

    @GetMapping("/stats")
    public ResponseEntity<StatsResponse> getStats() {
        return ResponseEntity.ok(sessionService.buildStats());
    }

    @GetMapping("/stats/weekly")
    public ResponseEntity<List<WeeklyFocusPoint>> getWeeklyStats(@RequestParam(defaultValue = "7") int days) {
        return ResponseEntity.ok(sessionService.buildWeeklyFocus(days));
    }

    @GetMapping("/summary")
    public ResponseEntity<String> getSummary() {
        return ResponseEntity.ok(sessionService.buildTextSummary());
    }

    @GetMapping(value = "/export", produces = "text/csv")
    public ResponseEntity<String> exportCsv(@RequestParam(required = false) String tag) {
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType("text/csv"))
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"focusflow-sessions.csv\"")
                .body(sessionService.exportSessionsCsv(tag));
    }
}
