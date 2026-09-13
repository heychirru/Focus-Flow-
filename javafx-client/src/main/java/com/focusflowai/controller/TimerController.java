package com.focusflowai.controller;

import com.focusflowai.model.Session;
import com.focusflowai.model.Tag;
import com.focusflowai.service.ApiClient;
import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ColorPicker;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.Spinner;
import javafx.scene.control.SpinnerValueFactory;
import javafx.scene.control.TextField;
import javafx.scene.paint.Color;
import javafx.util.Duration;

import java.net.URL;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

public class TimerController implements Initializable {

    @FXML
    private Label timerLabel;
    @FXML
    private Label statusLabel;
    @FXML
    private ComboBox<Tag> tagCombo;
    @FXML
    private Spinner<Integer> durationSpinner;
    @FXML
    private Button startBtn;
    @FXML
    private Button pauseBtn;
    @FXML
    private Button stopBtn;
    @FXML
    private Label streakLabel;
    @FXML
    private Label todayLabel;
    @FXML
    private ListView<Tag> tagListView;
    @FXML
    private TextField tagNameField;
    @FXML
    private ColorPicker tagColorPicker;
    @FXML
    private Label selectedTagPreview;

    private final ApiClient apiClient = ApiClient.getInstance();
    private Timeline timeline;
    private Long activeSessionId;
    private int remainingSeconds;
    private boolean paused;

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        durationSpinner.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(1, 120, 25));
        configureTagViews();
        tagColorPicker.setValue(Color.web("#4A90D9"));
        loadTags("General", null);
        loadStats();

        pauseBtn.setDisable(true);
        stopBtn.setDisable(true);
    }

    private void configureTagViews() {
        tagCombo.setCellFactory(list -> buildTagCell());
        tagCombo.setButtonCell(buildTagCell());
        tagListView.setCellFactory(list -> buildTagCell());

        tagCombo.valueProperty().addListener((obs, oldValue, newValue) -> updateSelectedTagPreview(newValue));
        tagListView.getSelectionModel().selectedItemProperty().addListener((obs, oldValue, newValue) -> populateTagEditor(newValue));
    }

    private ListCell<Tag> buildTagCell() {
        return new ListCell<>() {
            @Override
            protected void updateItem(Tag item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setStyle("");
                    return;
                }

                setText(item.name);
                setStyle("-fx-border-color: transparent transparent transparent " + item.color + ";" +
                        "-fx-border-width: 0 0 0 4;" +
                        "-fx-padding: 6 8 6 10;");
            }
        };
    }

    private void loadTags(String comboSelectionName, Long editorSelectionId) {
        apiClient.getListAsync("/api/sessions/tags",
                new com.google.gson.reflect.TypeToken<List<Tag>>() {
                })
                .thenAccept(tags -> Platform.runLater(() -> {
                    List<Tag> orderedTags = tags == null ? List.of() : tags.stream()
                            .sorted(Comparator.comparing(tag -> tag.name.toLowerCase()))
                            .toList();

                    tagCombo.setItems(FXCollections.observableArrayList(orderedTags));
                    tagListView.setItems(FXCollections.observableArrayList(orderedTags));

                    Tag comboSelection = findTagByName(orderedTags, comboSelectionName);
                    if (comboSelection == null && !orderedTags.isEmpty()) {
                        comboSelection = orderedTags.get(0);
                    }
                    tagCombo.setValue(comboSelection);

                    Tag editorSelection = findTagById(orderedTags, editorSelectionId);
                    if (editorSelection != null) {
                        tagListView.getSelectionModel().select(editorSelection);
                    } else if (tagListView.getSelectionModel().getSelectedItem() == null && !orderedTags.isEmpty()) {
                        tagListView.getSelectionModel().select(orderedTags.get(0));
                    }

                    if (comboSelection != null) {
                        updateSelectedTagPreview(comboSelection);
                    }
                }))
                .exceptionally(e -> {
                    Platform.runLater(() -> setStatus("Could not load tags", "#EF5350"));
                    return null;
                });
    }

    private Tag findTagByName(List<Tag> tags, String name) {
        if (name == null) {
            return null;
        }
        return tags.stream()
                .filter(tag -> name.equals(tag.name))
                .findFirst()
                .orElse(null);
    }

    private Tag findTagById(List<Tag> tags, Long id) {
        if (id == null) {
            return null;
        }
        return tags.stream()
                .filter(tag -> id.equals(tag.id))
                .findFirst()
                .orElse(null);
    }

    private void populateTagEditor(Tag tag) {
        if (tag == null) {
            tagNameField.clear();
            tagColorPicker.setValue(Color.web("#4A90D9"));
            return;
        }

        tagNameField.setText(tag.name);
        tagColorPicker.setValue(Color.web(tag.color));
    }

    private void updateSelectedTagPreview(Tag tag) {
        if (tag == null) {
            selectedTagPreview.setText("No tag selected");
            selectedTagPreview.setStyle("-fx-background-color: #21262D;");
            return;
        }

        selectedTagPreview.setText(tag.name + " " + tag.color);
        selectedTagPreview.setStyle("-fx-background-color: " + tag.color + ";" +
                "-fx-text-fill: white;" +
                "-fx-background-radius: 999;" +
                "-fx-padding: 8 14;");
    }

    private void loadStats() {
        apiClient.getAsync("/api/sessions/stats", com.focusflowai.model.StatsResponse.class)
                .thenAccept(stats -> Platform.runLater(() -> {
                    if (stats != null) {
                        streakLabel.setText("Streak: " + stats.currentStreak + " days");
                        todayLabel.setText("Today: " + stats.todayMinutes + " min");
                    }
                }))
                .exceptionally(e -> null);
    }

    @FXML
    public void onStart() {
        Tag selectedTag = tagCombo.getValue();
        int minutes = durationSpinner.getValue();

        setStatus("Starting session...", "#FFA726");
        startBtn.setDisable(true);

        apiClient.postAsync("/api/sessions",
                        Map.of("tag", selectedTag != null ? selectedTag.name : "General", "durationMinutes", minutes),
                        Session.class)
                .thenAccept(session -> Platform.runLater(() -> {
                    activeSessionId = session.id;
                    remainingSeconds = minutes * 60;
                    paused = false;
                    startCountdown();
                    setStatus("Stay focused.", "#4CAF50");
                    pauseBtn.setDisable(false);
                    stopBtn.setDisable(false);
                    tagCombo.setDisable(true);
                    durationSpinner.setDisable(true);
                }))
                .exceptionally(e -> {
                    Platform.runLater(() -> {
                        setStatus("Error: " + e.getMessage(), "#EF5350");
                        startBtn.setDisable(false);
                    });
                    return null;
                });
    }

    private void startCountdown() {
        if (timeline != null) {
            timeline.stop();
        }
        updateTimerDisplay();

        timeline = new Timeline(new KeyFrame(Duration.seconds(1), e -> {
            if (!paused) {
                remainingSeconds--;
                updateTimerDisplay();
                if (remainingSeconds <= 0) {
                    timeline.stop();
                    onSessionComplete();
                }
            }
        }));
        timeline.setCycleCount(Animation.INDEFINITE);
        timeline.play();
    }

    private void updateTimerDisplay() {
        int minutes = remainingSeconds / 60;
        int seconds = remainingSeconds % 60;
        timerLabel.setText(String.format("%02d:%02d", minutes, seconds));
    }

    @FXML
    public void onPause() {
        paused = !paused;
        pauseBtn.setText(paused ? "Resume" : "Pause");
        setStatus(paused ? "Paused" : "Stay focused.", paused ? "#FFA726" : "#4CAF50");
    }

    @FXML
    public void onStop() {
        if (activeSessionId == null) {
            return;
        }
        if (timeline != null) {
            timeline.stop();
        }

        apiClient.putAsync("/api/sessions/" + activeSessionId + "/cancel", null, Session.class)
                .thenAccept(session -> Platform.runLater(() -> {
                    resetUI();
                    setStatus("Session cancelled.", "#9E9E9E");
                    loadStats();
                }))
                .exceptionally(e -> {
                    Platform.runLater(() -> setStatus("Cancel failed: " + e.getMessage(), "#EF5350"));
                    return null;
                });
    }

    private void onSessionComplete() {
        if (activeSessionId == null) {
            return;
        }

        apiClient.putAsync("/api/sessions/" + activeSessionId + "/complete", null, Session.class)
                .thenAccept(session -> Platform.runLater(() -> {
                    resetUI();
                    setStatus("Session complete. Great work.", "#4CAF50");
                    showCompletionDialog();
                    loadStats();
                }))
                .exceptionally(e -> {
                    Platform.runLater(() -> setStatus("Complete failed: " + e.getMessage(), "#EF5350"));
                    return null;
                });
    }

    private void showCompletionDialog() {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Session Complete");
        alert.setHeaderText("Nice job");
        alert.setContentText("You completed your focus session. Take a short break, then come back strong.");
        alert.showAndWait();
    }

    @FXML
    public void onNewTag() {
        tagListView.getSelectionModel().clearSelection();
        populateTagEditor(null);
        setStatus("Enter a name and color, then click Create Tag.", "#8B949E");
    }

    @FXML
    public void onCreateTag() {
        submitTag(apiClient.postAsync("/api/sessions/tags", buildTagBody(), Tag.class), createdTag -> {
            setStatus("Created tag '" + createdTag.name + "'.", "#4CAF50");
            loadTags(createdTag.name, createdTag.id);
        });
    }

    @FXML
    public void onSaveTag() {
        Tag selectedTag = tagListView.getSelectionModel().getSelectedItem();
        if (selectedTag == null) {
            setStatus("Select a tag to update first.", "#FFA726");
            return;
        }

        submitTag(apiClient.putAsync("/api/sessions/tags/" + selectedTag.id, buildTagBody(), Tag.class), updatedTag -> {
            setStatus("Updated tag '" + updatedTag.name + "'.", "#4CAF50");
            loadTags(updatedTag.name, updatedTag.id);
        });
    }

    private Map<String, String> buildTagBody() {
        return Map.of(
                "name", tagNameField.getText() == null ? "" : tagNameField.getText().trim(),
                "color", toHex(tagColorPicker.getValue())
        );
    }

    private void submitTag(CompletableFuture<Tag> future, Consumer<Tag> onSuccess) {
        future.thenAccept(tag -> Platform.runLater(() -> onSuccess.accept(tag)))
                .exceptionally(e -> {
                    Platform.runLater(() -> setStatus("Tag save failed: " + e.getMessage(), "#EF5350"));
                    return null;
                });
    }

    private String toHex(Color color) {
        return String.format("#%02X%02X%02X",
                (int) Math.round(color.getRed() * 255),
                (int) Math.round(color.getGreen() * 255),
                (int) Math.round(color.getBlue() * 255));
    }

    private void resetUI() {
        activeSessionId = null;
        timerLabel.setText("00:00");
        startBtn.setDisable(false);
        pauseBtn.setDisable(true);
        pauseBtn.setText("Pause");
        stopBtn.setDisable(true);
        tagCombo.setDisable(false);
        durationSpinner.setDisable(false);
        paused = false;
    }

    private void setStatus(String msg, String color) {
        statusLabel.setText(msg);
        statusLabel.setStyle("-fx-text-fill: " + color + ";");
    }
}
