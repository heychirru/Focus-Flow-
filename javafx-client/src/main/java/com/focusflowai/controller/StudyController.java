package com.focusflowai.controller;

import com.focusflowai.model.Deck;
import com.focusflowai.model.Flashcard;
import com.focusflowai.service.ApiClient;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;

import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;

public class StudyController implements Initializable {

    @FXML
    private ListView<String> deckListView;
    @FXML
    private TextArea sourceTextArea;
    @FXML
    private TextField deckTitleField;
    @FXML
    private Button generateBtn;
    @FXML
    private Label statusLabel;
    @FXML
    private Label cardQuestionLabel;
    @FXML
    private Label cardAnswerLabel;
    @FXML
    private Button prevCardBtn;
    @FXML
    private Button nextCardBtn;
    @FXML
    private Button flipBtn;
    @FXML
    private Button knownBtn;
    @FXML
    private Label cardCountLabel;
    @FXML
    private Button quizModeBtn;
    @FXML
    private Label quizScoreLabel;
    @FXML
    private VBox quizBox;
    @FXML
    private Label quizPromptLabel;
    @FXML
    private Button optionBtn1;
    @FXML
    private Button optionBtn2;
    @FXML
    private Button optionBtn3;
    @FXML
    private Button optionBtn4;

    private final ApiClient apiClient = ApiClient.getInstance();
    private List<Deck> decks;
    private List<Flashcard> cards;
    private int currentCardIndex;
    private boolean showingAnswer;
    private boolean quizMode;
    private List<Flashcard> quizCards = List.of();
    private int quizQuestionIndex;
    private int quizScore;
    private boolean quizAnswered;
    private final List<Button> quizButtons = new ArrayList<>();

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        quizButtons.addAll(List.of(optionBtn1, optionBtn2, optionBtn3, optionBtn4));
        setQuizMode(false);
        loadDecks();

        deckListView.getSelectionModel().selectedIndexProperty().addListener((obs, oldValue, newValue) -> {
            if (newValue.intValue() >= 0 && decks != null && newValue.intValue() < decks.size()) {
                loadCards(decks.get(newValue.intValue()).id);
            }
        });
    }

    private void loadDecks() {
        apiClient.getListAsync("/api/study/decks",
                new com.google.gson.reflect.TypeToken<List<Deck>>() {
                })
                .thenAccept(list -> Platform.runLater(() -> {
                    decks = list;
                    deckListView.getItems().clear();
                    if (list != null) {
                        list.forEach(deck -> deckListView.getItems().add(deck.title));
                    }
                }))
                .exceptionally(e -> null);
    }

    @FXML
    public void onGenerate() {
        String title = deckTitleField.getText().trim();
        String text = sourceTextArea.getText().trim();

        if (title.isEmpty() || text.length() < 50) {
            statusLabel.setText("Title required and source text must be at least 50 chars.");
            return;
        }

        generateBtn.setDisable(true);
        statusLabel.setText("Generating flashcards. This can take a few seconds.");

        apiClient.postAsync("/api/study/decks/generate",
                        Map.of("title", title, "sourceText", text), Deck.class)
                .thenAccept(deck -> Platform.runLater(() -> {
                    generateBtn.setDisable(false);
                    statusLabel.setText("Created '" + deck.title + "' with " +
                            (deck.flashcards != null ? deck.flashcards.size() : 0) + " cards.");
                    deckTitleField.clear();
                    sourceTextArea.clear();
                    loadDecks();
                }))
                .exceptionally(e -> {
                    Platform.runLater(() -> {
                        generateBtn.setDisable(false);
                        statusLabel.setText("Error: " + e.getMessage());
                    });
                    return null;
                });
    }

    private void loadCards(Long deckId) {
        apiClient.getListAsync("/api/study/decks/" + deckId + "/cards",
                new com.google.gson.reflect.TypeToken<List<Flashcard>>() {
                })
                .thenAccept(list -> Platform.runLater(() -> {
                    cards = list;
                    currentCardIndex = 0;
                    showingAnswer = false;
                    setQuizMode(false);
                    displayCurrentCard();
                }))
                .exceptionally(e -> null);
    }

    private void displayCurrentCard() {
        if (quizMode) {
            displayQuizQuestion();
            return;
        }

        if (cards == null || cards.isEmpty()) {
            cardQuestionLabel.setText("Select a deck to study");
            cardAnswerLabel.setText("");
            cardCountLabel.setText("0/0");
            knownBtn.setText("Mark as Known");
            return;
        }

        Flashcard card = cards.get(currentCardIndex);
        cardQuestionLabel.setText(card.question);
        cardAnswerLabel.setText(showingAnswer ? card.answer : "");
        cardCountLabel.setText((currentCardIndex + 1) + "/" + cards.size());
        flipBtn.setText(showingAnswer ? "Show Question" : "Show Answer");
        knownBtn.setText(card.known ? "Known" : "Mark as Known");
    }

    @FXML
    public void onFlip() {
        if (quizMode || cards == null || cards.isEmpty()) {
            return;
        }
        showingAnswer = !showingAnswer;
        displayCurrentCard();
    }

    @FXML
    public void onPrevCard() {
        if (quizMode || cards == null || cards.isEmpty()) {
            return;
        }
        currentCardIndex = (currentCardIndex - 1 + cards.size()) % cards.size();
        showingAnswer = false;
        displayCurrentCard();
    }

    @FXML
    public void onNextCard() {
        if (cards == null || cards.isEmpty()) {
            return;
        }

        if (quizMode) {
            if (!quizAnswered) {
                statusLabel.setText("Pick an answer before moving on.");
                return;
            }
            if (quizQuestionIndex >= quizCards.size() - 1) {
                finishQuiz();
                return;
            }
            quizQuestionIndex++;
            quizAnswered = false;
            displayQuizQuestion();
            return;
        }

        currentCardIndex = (currentCardIndex + 1) % cards.size();
        showingAnswer = false;
        displayCurrentCard();
    }

    @FXML
    public void onMarkKnown() {
        if (quizMode || cards == null || cards.isEmpty()) {
            return;
        }

        Flashcard card = cards.get(currentCardIndex);
        boolean newKnown = !card.known;

        apiClient.patchAsync("/api/study/cards/" + card.id + "/known",
                        Map.of("known", newKnown), Flashcard.class)
                .thenAccept(updated -> Platform.runLater(() -> {
                    card.known = updated.known;
                    knownBtn.setText(card.known ? "Known" : "Mark as Known");
                    statusLabel.setText(card.known ? "Card marked as known." : "Card marked for review.");
                }))
                .exceptionally(e -> null);
    }

    @FXML
    public void onDeleteDeck() {
        int idx = deckListView.getSelectionModel().getSelectedIndex();
        if (idx < 0 || decks == null) {
            return;
        }

        Deck deck = decks.get(idx);
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
                "Delete deck '" + deck.title + "' and all its cards?",
                ButtonType.YES, ButtonType.NO);
        confirm.showAndWait().ifPresent(button -> {
            if (button == ButtonType.YES) {
                apiClient.deleteAsync("/api/study/decks/" + deck.id)
                        .thenRun(() -> Platform.runLater(() -> {
                            statusLabel.setText("Deck deleted.");
                            loadDecks();
                            cards = List.of();
                            setQuizMode(false);
                            displayCurrentCard();
                        }))
                        .exceptionally(e -> {
                            Platform.runLater(() -> statusLabel.setText("Delete failed."));
                            return null;
                        });
            }
        });
    }

    @FXML
    public void onToggleQuiz() {
        if (cards == null || cards.isEmpty()) {
            statusLabel.setText("Select a deck before starting quiz mode.");
            return;
        }

        if (quizMode) {
            setQuizMode(false);
            displayCurrentCard();
            return;
        }

        quizCards = new ArrayList<>(cards);
        Collections.shuffle(quizCards);
        quizQuestionIndex = 0;
        quizScore = 0;
        quizAnswered = false;
        setQuizMode(true);
        displayQuizQuestion();
    }

    @FXML
    public void onQuizOption(ActionEvent event) {
        if (!quizMode || quizAnswered || quizCards.isEmpty()) {
            return;
        }

        Button clickedButton = (Button) event.getSource();
        Flashcard currentCard = quizCards.get(quizQuestionIndex);
        boolean correct = currentCard.answer.equals(clickedButton.getText());
        quizAnswered = true;

        if (correct) {
            quizScore++;
            clickedButton.setStyle("-fx-background-color: #2E7D32; -fx-text-fill: white;");
            statusLabel.setText("Correct.");
        } else {
            clickedButton.setStyle("-fx-background-color: #8B1E3F; -fx-text-fill: white;");
            revealCorrectAnswer(currentCard.answer);
            statusLabel.setText("Not quite. The correct answer is highlighted.");
        }

        quizScoreLabel.setText("Score: " + quizScore + "/" + quizCards.size());
        quizButtons.forEach(button -> button.setDisable(true));
    }

    private void displayQuizQuestion() {
        if (!quizMode || quizCards.isEmpty()) {
            return;
        }

        Flashcard currentCard = quizCards.get(quizQuestionIndex);
        cardQuestionLabel.setText(currentCard.question);
        cardAnswerLabel.setText("");
        cardCountLabel.setText("Quiz " + (quizQuestionIndex + 1) + "/" + quizCards.size());
        quizPromptLabel.setText("Pick the best answer.");
        quizScoreLabel.setText("Score: " + quizScore + "/" + quizCards.size());

        List<String> answers = new ArrayList<>(new LinkedHashSet<>(
                cards.stream().map(card -> card.answer).toList()));
        answers.remove(currentCard.answer);
        Collections.shuffle(answers);

        List<String> options = new ArrayList<>();
        options.add(currentCard.answer);
        options.addAll(answers.stream().limit(3).toList());
        Collections.shuffle(options);

        for (int i = 0; i < quizButtons.size(); i++) {
            Button button = quizButtons.get(i);
            if (i < options.size()) {
                button.setText(options.get(i));
                button.setVisible(true);
                button.setManaged(true);
                button.setDisable(false);
                button.setStyle("");
            } else {
                button.setVisible(false);
                button.setManaged(false);
            }
        }
    }

    private void revealCorrectAnswer(String answer) {
        quizButtons.stream()
                .filter(button -> answer.equals(button.getText()))
                .findFirst()
                .ifPresent(button -> button.setStyle("-fx-background-color: #2E7D32; -fx-text-fill: white;"));
    }

    private void finishQuiz() {
        setQuizMode(false);
        Alert result = new Alert(Alert.AlertType.INFORMATION);
        result.setTitle("Quiz Complete");
        result.setHeaderText("Deck quiz finished");
        result.setContentText("You scored " + quizScore + " out of " + quizCards.size() + ".");
        result.showAndWait();
        displayCurrentCard();
    }

    private void setQuizMode(boolean enabled) {
        quizMode = enabled;
        quizBox.setVisible(enabled);
        quizBox.setManaged(enabled);
        quizScoreLabel.setVisible(enabled);
        quizScoreLabel.setManaged(enabled);
        knownBtn.setDisable(enabled);
        flipBtn.setDisable(enabled);
        prevCardBtn.setDisable(enabled);
        quizModeBtn.setText(enabled ? "Exit Quiz" : "Start Quiz");
        nextCardBtn.setText(enabled ? "Next Question" : "Next");
    }
}
