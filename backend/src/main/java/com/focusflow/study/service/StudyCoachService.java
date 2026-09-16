package com.focusflow.study.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.focusflow.study.client.StudyClaudeApiClient;
import com.focusflow.study.dto.GenerateDeckRequest;
import com.focusflow.study.dto.UpdateKnownRequest;
import com.focusflow.study.model.Deck;
import com.focusflow.study.model.Flashcard;
import com.focusflow.study.repository.DeckRepository;
import com.focusflow.study.repository.FlashcardRepository;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

@Service
public class StudyCoachService {

    private static final String SYSTEM_PROMPT = """
            You are a study assistant that creates high-quality flashcards.
            Return ONLY valid JSON — no explanation, no markdown fences.
            The JSON format must be exactly:
            {"cards":[{"question":"...","answer":"..."}]}
            Generate between 5-10 flashcards covering the key concepts.
            """;

    private final DeckRepository deckRepository;
    private final FlashcardRepository flashcardRepository;
    private final StudyClaudeApiClient claudeClient;

    public StudyCoachService(DeckRepository deckRepository,
                             FlashcardRepository flashcardRepository,
                             StudyClaudeApiClient claudeClient) {
        this.deckRepository = deckRepository;
        this.flashcardRepository = flashcardRepository;
        this.claudeClient = claudeClient;
    }

    public List<Deck> findAllDecks() {
        return deckRepository.findAllByOrderByCreatedAtDesc();
    }

    public Deck findDeckById(Long id) {
        return deckRepository.findByIdWithCards(id)
                .orElseThrow(() -> new RuntimeException("Deck with id " + id + " not found"));
    }

    @Transactional
    public Deck generateDeck(GenerateDeckRequest req) {
        if (req.sourceText() == null || req.sourceText().length() < 50) {
            throw new IllegalArgumentException("sourceText must be at least 50 characters");
        }

        String userPrompt = "Generate flashcards from this text:\n\n" + req.sourceText();
        String rawJson = claudeClient.complete(SYSTEM_PROMPT, userPrompt);

        JsonObject responseObj = JsonParser.parseString(rawJson.trim()).getAsJsonObject();
        JsonArray cards = responseObj.getAsJsonArray("cards");

        if (cards == null || cards.isEmpty()) {
            throw new RuntimeException("Claude returned no flashcards");
        }

        Deck deck = new Deck();
        deck.setTitle(req.title());
        deck = deckRepository.save(deck);

        for (var element : cards) {
            JsonObject card = element.getAsJsonObject();
            Flashcard fc = new Flashcard();
            fc.setDeck(deck);
            fc.setQuestion(card.get("question").getAsString());
            fc.setAnswer(card.get("answer").getAsString());
            deck.getFlashcards().add(flashcardRepository.save(fc));
        }

        return deckRepository.findByIdWithCards(deck.getId())
                .orElseThrow(() -> new RuntimeException("Deck save failed"));
    }

    @Transactional
    public void deleteDeck(Long id) {
        if (!deckRepository.existsById(id)) {
            throw new RuntimeException("Deck with id " + id + " not found");
        }
        deckRepository.deleteById(id);
    }

    public List<Flashcard> findCardsByDeck(Long deckId) {
        if (!deckRepository.existsById(deckId)) {
            throw new RuntimeException("Deck with id " + deckId + " not found");
        }
        return flashcardRepository.findByDeckId(deckId);
    }

    @Transactional
    public Flashcard updateKnown(Long cardId, UpdateKnownRequest req) {
        Flashcard card = flashcardRepository.findById(cardId)
                .orElseThrow(() -> new RuntimeException("Flashcard with id " + cardId + " not found"));
        card.setKnown(req.known());
        return flashcardRepository.save(card);
    }
}
