package com.focusflowai.controller;

import com.focusflowai.model.Session;
import com.focusflowai.model.Tag;
import com.focusflowai.service.ApiClient;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.FileChooser;

import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.List;
import java.util.ResourceBundle;

public class HistoryController implements Initializable {

    private static final int PAGE_SIZE = 20;

    @FXML
    private TableView<SessionRow> historyTable;
    @FXML
    private TableColumn<SessionRow, Long> idCol;
    @FXML
    private TableColumn<SessionRow, String> tagCol;
    @FXML
    private TableColumn<SessionRow, Integer> durationCol;
    @FXML
    private TableColumn<SessionRow, String> startCol;
    @FXML
    private TableColumn<SessionRow, String> statusCol;
    @FXML
    private Label statusLabel;
    @FXML
    private Label pageLabel;
    @FXML
    private Button prevBtn;
    @FXML
    private Button nextBtn;
    @FXML
    private ComboBox<String> tagFilterCombo;

    private final ApiClient apiClient = ApiClient.getInstance();
    private int currentOffset;
    private int lastResultSize;

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        idCol.setCellValueFactory(new PropertyValueFactory<>("id"));
        tagCol.setCellValueFactory(new PropertyValueFactory<>("tag"));
        durationCol.setCellValueFactory(new PropertyValueFactory<>("durationMinutes"));
        startCol.setCellValueFactory(new PropertyValueFactory<>("startTime"));
        statusCol.setCellValueFactory(new PropertyValueFactory<>("status"));

        tagFilterCombo.getItems().add("All Tags");
        tagFilterCombo.setValue("All Tags");
        tagFilterCombo.valueProperty().addListener((obs, oldValue, newValue) -> {
            currentOffset = 0;
            loadHistory();
        });

        loadTags();
        loadHistory();
    }

    private void loadTags() {
        apiClient.getListAsync("/api/sessions/tags",
                new com.google.gson.reflect.TypeToken<List<Tag>>() {
                })
                .thenAccept(tags -> Platform.runLater(() -> {
                    tagFilterCombo.getItems().setAll("All Tags");
                    if (tags != null) {
                        tags.stream().map(tag -> tag.name).sorted().forEach(tagFilterCombo.getItems()::add);
                    }
                    if (tagFilterCombo.getValue() == null) {
                        tagFilterCombo.setValue("All Tags");
                    }
                }))
                .exceptionally(e -> null);
    }

    @FXML
    public void onRefresh() {
        loadHistory();
    }

    @FXML
    public void onPrevPage() {
        if (currentOffset >= PAGE_SIZE) {
            currentOffset -= PAGE_SIZE;
            loadHistory();
        }
    }

    @FXML
    public void onNextPage() {
        if (lastResultSize == PAGE_SIZE) {
            currentOffset += PAGE_SIZE;
            loadHistory();
        }
    }

    @FXML
    public void onExportCsv() {
        String path = buildHistoryPath("/api/sessions/export");
        statusLabel.setText("Preparing CSV export...");

        apiClient.getTextAsync(path)
                .thenAccept(csv -> Platform.runLater(() -> {
                    try {
                        FileChooser chooser = new FileChooser();
                        chooser.setTitle("Save Session History");
                        chooser.setInitialFileName("focusflow-sessions.csv");
                        chooser.getExtensionFilters().setAll(new FileChooser.ExtensionFilter("CSV Files", "*.csv"));

                        java.io.File selectedFile = chooser.showSaveDialog(historyTable.getScene().getWindow());
                        if (selectedFile == null) {
                            statusLabel.setText("Export cancelled.");
                            return;
                        }

                        Files.writeString(selectedFile.toPath(), csv, StandardCharsets.UTF_8);
                        statusLabel.setText("Exported CSV to " + selectedFile.getName());
                    } catch (Exception e) {
                        statusLabel.setText("Export failed: " + e.getMessage());
                    }
                }))
                .exceptionally(e -> {
                    Platform.runLater(() -> statusLabel.setText("Export failed: " + e.getMessage()));
                    return null;
                });
    }

    private void loadHistory() {
        statusLabel.setText("Loading history...");
        apiClient.getListAsync(buildHistoryPath("/api/sessions"),
                new com.google.gson.reflect.TypeToken<List<Session>>() {
                })
                .thenAccept(sessions -> Platform.runLater(() -> {
                    historyTable.getItems().clear();
                    lastResultSize = sessions == null ? 0 : sessions.size();

                    if (sessions == null || sessions.isEmpty()) {
                        statusLabel.setText(currentOffset == 0 ? "No sessions yet." : "No more sessions.");
                    } else {
                        sessions.stream().map(SessionRow::new).forEach(historyTable.getItems()::add);
                        statusLabel.setText("Showing " + sessions.size() + " sessions");
                    }

                    pageLabel.setText("Page " + ((currentOffset / PAGE_SIZE) + 1));
                    prevBtn.setDisable(currentOffset == 0);
                    nextBtn.setDisable(lastResultSize < PAGE_SIZE);
                }))
                .exceptionally(e -> {
                    Platform.runLater(() -> statusLabel.setText("Error: " + e.getMessage()));
                    return null;
                });
    }

    private String buildHistoryPath(String basePath) {
        StringBuilder path = new StringBuilder(basePath)
                .append("?limit=").append(PAGE_SIZE)
                .append("&offset=").append(currentOffset);

        String selectedTag = tagFilterCombo.getValue();
        if (selectedTag != null && !"All Tags".equals(selectedTag)) {
            path.append("&tag=").append(URLEncoder.encode(selectedTag, StandardCharsets.UTF_8));
        }

        return path.toString();
    }

    public static class SessionRow {
        private final Long id;
        private final String tag;
        private final int durationMinutes;
        private final String startTime;
        private final String status;

        SessionRow(Session session) {
            this.id = session.id;
            this.tag = session.tag;
            this.durationMinutes = session.durationMinutes;
            this.startTime = session.startTime != null ? session.startTime.substring(0, 16).replace("T", " ") : "";
            this.status = session.completed ? "Completed" : "Cancelled";
        }

        public Long getId() {
            return id;
        }

        public String getTag() {
            return tag;
        }

        public int getDurationMinutes() {
            return durationMinutes;
        }

        public String getStartTime() {
            return startTime;
        }

        public String getStatus() {
            return status;
        }
    }
}
