package org.example.ui;

import javafx.application.Platform;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.layout.VBox;
import org.example.Model.Customer;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class PriorityPanel extends VBox {
    private final TableView<Customer> table;
    private final ScheduledExecutorService scheduler =
            Executors.newSingleThreadScheduledExecutor(r -> {
                Thread t = new Thread(r, "priority-panel-refresher");
                t.setDaemon(true);
                return t;
            });

    public PriorityPanel(ObservableList<Customer> customers) {
        setSpacing(10);
        setPadding(new Insets(10));

        table = new TableView<>(customers);

        // Müşteri adı
        TableColumn<Customer, String> nameCol = new TableColumn<>("Müşteri");
        nameCol.setCellValueFactory(data -> data.getValue().nameProperty());
        nameCol.setPrefWidth(200);

        // Tür (👑 Premium / 👤 Standard)
        TableColumn<Customer, String> typeCol = new TableColumn<>("Tür");
        typeCol.setCellValueFactory(data -> data.getValue().typeProperty());
        typeCol.setPrefWidth(120);
        typeCol.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(String s, boolean empty) {
                super.updateItem(s, empty);
                if (empty || s == null) { setText(null); setGraphic(null); return; }
                String icon = "Premium".equalsIgnoreCase(s) ? "👑 " : "👤 ";
                Label chip = new Label(icon + s);
                chip.getStyleClass().addAll("chip",
                        "Premium".equalsIgnoreCase(s) ? "chip-success" : "chip-idle");
                setGraphic(chip);
                setText(null);
                setStyle("-fx-alignment: CENTER;");
            }
        });

        // Bekleme (mm:ss)
        TableColumn<Customer, Number> waitingCol = new TableColumn<>("Bekleme");
        waitingCol.setCellValueFactory(data -> data.getValue().waitingTimeProperty());
        waitingCol.setPrefWidth(110);
        waitingCol.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(Number n, boolean empty) {
                super.updateItem(n, empty);
                if (empty || n == null) { setText(null); return; }
                int total = n.intValue();
                int mm = total / 60, ss = total % 60;
                setText(String.format("%02d:%02d", mm, ss));
                setStyle("-fx-alignment: CENTER;");
            }
        });

        // Öncelik (1 ondalık)
        TableColumn<Customer, Number> scoreCol = new TableColumn<>("Skor");
        scoreCol.setCellValueFactory(data -> data.getValue().priorityScoreProperty());
        scoreCol.setPrefWidth(90);
        scoreCol.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(Number n, boolean empty) {
                super.updateItem(n, empty);
                if (empty || n == null) { setText(null); return; }
                setText(String.format("%.1f", n.doubleValue()));
                setStyle("-fx-alignment: CENTER;");
            }
        });

        table.getColumns().addAll(nameCol, typeCol, waitingCol, scoreCol);

        // Varsayılan: Skor azalan sırada
        scoreCol.setSortType(TableColumn.SortType.DESCENDING);
        table.getSortOrder().add(scoreCol);

        getChildren().add(table);

        startAutoUpdate();
    }

    // Sadece tabloyu yenile + sıralamayı uygula (bekleme/öncelik servis tarafından güncelleniyor)
    private void startAutoUpdate() {
        scheduler.scheduleAtFixedRate(() -> {
            Platform.runLater(() -> {
                table.sort();    // skor değiştikçe sıra güncellensin
                table.refresh(); // hücreler tazelensin
            });
        }, 1, 1, TimeUnit.SECONDS);
    }

    public void refresh() {
        Platform.runLater(() -> {
            table.sort();
            table.refresh();
        });
    }
}
