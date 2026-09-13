package com.focusflowai.controller;

import com.focusflowai.model.StatsResponse;
import com.focusflowai.model.WeeklyFocusPoint;
import com.focusflowai.service.ApiClient;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;

import java.net.URL;
import java.util.List;
import java.util.ResourceBundle;

public class StatsController implements Initializable {

    @FXML
    private Label totalSessionsLabel;
    @FXML
    private Label totalHoursLabel;
    @FXML
    private Label todayMinLabel;
    @FXML
    private Label streakLabel;
    @FXML
    private Label completionRateLabel;
    @FXML
    private VBox topTagsBox;
    @FXML
    private Label statusLabel;
    @FXML
    private BarChart<String, Number> weeklyChart;

    private final ApiClient apiClient = ApiClient.getInstance();

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        loadStats();
    }

    @FXML
    public void onRefresh() {
        loadStats();
    }

    private void loadStats() {
        statusLabel.setText("Loading stats...");
        apiClient.getAsync("/api/sessions/stats", StatsResponse.class)
                .thenAccept(stats -> Platform.runLater(() -> {
                    if (stats == null) {
                        statusLabel.setText("No data yet. Start your first session.");
                        return;
                    }

                    totalSessionsLabel.setText(String.valueOf(stats.totalSessions));
                    totalHoursLabel.setText(String.format("%.1f hrs", stats.totalMinutes / 60.0));
                    todayMinLabel.setText(stats.todayMinutes + " min");
                    streakLabel.setText(stats.currentStreak + " days");
                    completionRateLabel.setText(String.format("%.0f%%", stats.completionRate * 100));

                    topTagsBox.getChildren().clear();
                    if (stats.topTags != null) {
                        stats.topTags.forEach(tag -> {
                            Label row = new Label(String.format("%-12s  %d sessions  %d min",
                                    tag.tag, tag.sessions, tag.totalMinutes));
                            row.getStyleClass().add("tag-stat-row");
                            topTagsBox.getChildren().add(row);
                        });
                    }

                    statusLabel.setText("");
                }))
                .exceptionally(e -> {
                    Platform.runLater(() -> statusLabel.setText("Error loading stats: " + e.getMessage()));
                    return null;
                });

        apiClient.getListAsync("/api/sessions/stats/weekly?days=7",
                        new com.google.gson.reflect.TypeToken<List<WeeklyFocusPoint>>() {
                        })
                .thenAccept(points -> Platform.runLater(() -> populateWeeklyChart(points)))
                .exceptionally(e -> {
                    Platform.runLater(() -> statusLabel.setText("Chart failed: " + e.getMessage()));
                    return null;
                });
    }

    private void populateWeeklyChart(List<WeeklyFocusPoint> points) {
        XYChart.Series<String, Number> minutesSeries = new XYChart.Series<>();
        minutesSeries.setName("Focus Minutes");

        if (points != null) {
            points.forEach(point -> minutesSeries.getData().add(
                    new XYChart.Data<>(point.date.substring(5), point.focusMinutes)));
        }

        weeklyChart.getData().setAll(minutesSeries);
    }
}
