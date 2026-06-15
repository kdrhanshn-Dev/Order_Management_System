package org.example.ui;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.layout.VBox;

import java.util.function.Consumer;

public class LogPanel extends VBox {
    private final ObservableList<Label> logs;
    private final ListView<Label> logList;

    public LogPanel() {
        this.logs = FXCollections.observableArrayList();
        this.logList = new ListView<>(logs);
        getChildren().add(logList);
    }

    // Logları UI'ya ekleme (UI thread güvenli)
    public Consumer<String> getLogConsumer() {
        return msg -> Platform.runLater(() -> {
            Label label = new Label(msg);

            if (msg.contains("✅") || msg.toUpperCase().contains("SUCCESS") || msg.toUpperCase().contains("APPROVED")) {
                label.setStyle("-fx-text-fill: green; -fx-font-weight: bold;");
            } else if (msg.contains("❌") || msg.toUpperCase().contains("FAIL") || msg.toUpperCase().contains("REJECTED")) {
                label.setStyle("-fx-text-fill: red; -fx-font-weight: bold;");
            } else if (msg.contains("⏳") || msg.toUpperCase().contains("TIMEOUT")) {
                label.setStyle("-fx-text-fill: orange; -fx-font-weight: bold;");
            } else {
                label.setStyle("-fx-text-fill: black;");
            }

            logs.add(0, label); // en yeni log en üstte
            // Auto-scroll (üstten eklediğimiz için seçimi sıfırla)
            logList.getSelectionModel().clearSelection();
        });
    }
}
