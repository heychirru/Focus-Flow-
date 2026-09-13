package com.focusflowai.controller;

import java.net.URL;
import java.util.ResourceBundle;

import com.focusflowai.model.InsightsResponse;
import com.focusflowai.service.ApiClient;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;

public class AiCoachController implements Initializable {

    @FXML
    private TextArea insightsArea;
    @FXML
    private Label statusLabel;
    @FXML
    private Label generatedAtLabel;
    @FXML
    private Button generateBtn;

    private final ApiClient apiClient = ApiClient.getInstance();

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        // Try to load cached insights on startup
        loadLatestInsights();
    }

    private void loadLatestInsights() {
        apiClient.getAsync("/api/coach/insights/latest", InsightsResponse.class)
                .thenAccept(resp -> Platform.runLater(() -> {
                    if (resp != null) {
                        displayInsights(resp);
                    } else {
                        statusLabel.setText("No insights yet. Click 'Get AI Coaching' to generate.");
                    }
                }))
                .exceptionally(e -> null);
    }

    @FXML
    public void onGenerate() {
        generateBtn.setDisable(true);
        statusLabel.setText("🤖 Asking Claude for insights… (may take 10-15 seconds)");
        insightsArea.clear();

        apiClient.postAsync("/api/coach/insights", null, InsightsResponse.class)
                .thenAccept(resp -> Platform.runLater(() -> {
                    displayInsights(resp);
                    generateBtn.setDisable(false);
                }))
                .exceptionally(e -> {
                    Platform.runLater(() -> {
                        statusLabel.setText("Error: " + e.getMessage());
                        generateBtn.setDisable(false);
                    });
                    return null;
                });
    }

    private void displayInsights(InsightsResponse resp) {
        insightsArea.setText(resp.insights);
        statusLabel.setText("✅ Insights ready");
        String time = resp.generatedAt != null ? resp.generatedAt.substring(0, 16).replace("T", " ") : "";
        generatedAtLabel.setText("Generated: " + time + " · Based on " + resp.sessionCount + " sessions");
    }
}
