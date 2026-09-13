package com.focusflowai.controller;

import java.io.IOException;
import java.net.URL;
import java.util.ResourceBundle;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.layout.StackPane;

public class MainController implements Initializable {

    @FXML
    private StackPane contentPane;

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        showTimer();
    }

    @FXML
    public void showTimer() {
        loadView("/fxml/TimerView.fxml");
    }

    @FXML
    public void showStats() {
        loadView("/fxml/StatsView.fxml");
    }

    @FXML
    public void showHistory() {
        loadView("/fxml/HistoryView.fxml");
    }

    @FXML
    public void showCoach() {
        loadView("/fxml/CoachView.fxml");
    }

    @FXML
    public void showStudy() {
        loadView("/fxml/StudyView.fxml");
    }

    private void loadView(String fxmlPath) {
        try {
            Node view = FXMLLoader.load(getClass().getResource(fxmlPath));
            contentPane.getChildren().setAll(view);
        } catch (IOException e) {
            throw new RuntimeException("Failed to load view: " + fxmlPath, e);
        }
    }
}
