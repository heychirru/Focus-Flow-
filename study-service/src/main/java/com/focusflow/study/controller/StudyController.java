package com.focusflow.study.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.focusflow.study.dto.GenerateDeckRequest;
import com.focusflow.study.dto.UpdateKnownRequest;
import com.focusflow.study.model.Deck;
import com.focusflow.study.model.Flashcard;
import com.focusflow.study.service.StudyCoachService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/study")
@RequiredArgsConstructor
public class StudyController {

    private final StudyCoachService studyService;

    /** GET /api/study/decks — list all decks */
    @GetMapping("/decks")
    public ResponseEntity<List<Deck>> getDecks() {
        return ResponseEntity.ok(studyService.findAllDecks());
    }

    /**
     * POST /api/study/decks/generate — generate deck from source text via Claude
     */
    @PostMapping("/decks/generate")
    public ResponseEntity<Deck> generateDeck(@RequestBody GenerateDeckRequest req) {
        Deck deck = studyService.generateDeck(req);
        return ResponseEntity.status(HttpStatus.CREATED).body(deck);
    }

    /** GET /api/study/decks/{id}/cards — get all cards for a deck */
    @GetMapping("/decks/{id}/cards")
    public ResponseEntity<List<Flashcard>> getCards(@PathVariable Long id) {
        return ResponseEntity.ok(studyService.findCardsByDeck(id));
    }

    /** DELETE /api/study/decks/{id} — delete deck and all cards */
    @DeleteMapping("/decks/{id}")
    public ResponseEntity<Void> deleteDeck(@PathVariable Long id) {
        studyService.deleteDeck(id);
        return ResponseEntity.noContent().build();
    }

    /** PATCH /api/study/cards/{id}/known — update known status of a card */
    @PatchMapping("/cards/{id}/known")
    public ResponseEntity<Flashcard> updateKnown(@PathVariable Long id,
            @RequestBody UpdateKnownRequest req) {
        return ResponseEntity.ok(studyService.updateKnown(id, req));
    }
}
