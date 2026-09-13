package com.focusflow.session.controller;

import com.focusflow.session.dto.CreateTagRequest;
import com.focusflow.session.dto.UpdateTagRequest;
import com.focusflow.session.model.Tag;
import com.focusflow.session.service.SessionService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/sessions/tags")
public class TagController {

    private final SessionService sessionService;

    public TagController(SessionService sessionService) {
        this.sessionService = sessionService;
    }

    @GetMapping
    public ResponseEntity<List<Tag>> getTags() {
        return ResponseEntity.ok(sessionService.findAllTags());
    }

    @PostMapping
    public ResponseEntity<Tag> createTag(@RequestBody CreateTagRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(sessionService.createTag(req));
    }

    @PutMapping("/{id}")
    public ResponseEntity<Tag> updateTag(@PathVariable Long id, @RequestBody UpdateTagRequest req) {
        return ResponseEntity.ok(sessionService.updateTag(id, req));
    }
}
