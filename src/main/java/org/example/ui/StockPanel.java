package org.example.ui;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.scene.chart.PieChart;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.layout.VBox;
import org.example.Model.Product;
import org.example.Repository.ProductRepository;

import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class StockPanel extends VBox {
    private final ProductRepository productRepo;

    // Table data
    private final ObservableList<Product> products;
    private final TableView<Product> table;

    // Chart data (tek instance!)
    private final PieChart pieChart = new PieChart();
    private final ObservableList<PieChart.Data> pieData = FXCollections.observableArrayList();

    // Opsiyonel progress gösterge
    private final ProgressBar progress = new ProgressBar(0);

    // Periyodik güncelleme
    private final ScheduledExecutorService exec =
            Executors.newSingleThreadScheduledExecutor(r -> {
                Thread t = new Thread(r, "stock-panel-updater");
                t.setDaemon(true);
                return t;
            });

    public StockPanel(ProductRepository productRepo) {
        this.productRepo = productRepo;

        // tabloyu ilk verilerle doldur
        this.products = FXCollections.observableArrayList(productRepo.getAllProducts());
        this.table = new TableView<>(products);

        setSpacing(10);
        setPadding(new Insets(10));

        // --- Tablo kolonları ---
        TableColumn<Product, Number> idCol = new TableColumn<>("ID");
        idCol.setCellValueFactory(data -> data.getValue().idProperty());
        idCol.setPrefWidth(60);

        TableColumn<Product, String> nameCol = new TableColumn<>("Ürün");
        nameCol.setCellValueFactory(data -> data.getValue().nameProperty());
        nameCol.setPrefWidth(240);

        TableColumn<Product, Number> stockCol = new TableColumn<>("Stok");
        stockCol.setCellValueFactory(data -> data.getValue().stockProperty());
        stockCol.setPrefWidth(100);

        // Kritik stok renklendirme
        stockCol.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(Number item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setStyle("");
                } else {
                    setText(item.toString());
                    if (item.intValue() <= 0) {
                        setStyle("-fx-background-color: #8b0000; -fx-text-fill: white;"); // bitti
                    } else if (item.intValue() <= 10) {
                        setStyle("-fx-background-color: #d2691e; -fx-text-fill: white;"); // kritik
                    } else {
                        setStyle("");
                    }
                }
            }
        });

        TableColumn<Product, Number> priceCol = new TableColumn<>("Fiyat");
        priceCol.setCellValueFactory(data -> data.getValue().priceProperty());
        priceCol.setPrefWidth(120);

        table.getColumns().addAll(idCol, nameCol, stockCol, priceCol);
        table.setPrefHeight(360);

        // --- PieChart tek kez bağlanır ---
        pieChart.setTitle("Stok Dağılımı");
        pieChart.setData(pieData);        // 🔴 tek bağlama
        pieChart.setAnimated(false);// (tercih) animasyon kapalı
        pieChart.setLabelsVisible(true);   // dilim değerleri
        pieChart.setLegendVisible(true);   // lejand görünür

        // pieChart.setLegendVisible(false); // istersen kapat

        progress.setVisible(false);
        progress.setPrefWidth(240);

        getChildren().addAll(table, pieChart, progress);

        // İlk veri ve periyodik güncelleme
        updateData();
        startAutoRefresh();
    }

    // Repository'den oku, tablo ve grafiği tek hamlede güncelle
    private void updateData() {
        List<Product> fresh = productRepo.getAllProducts();

        int inStock = 0, lowStock = 0, zeroStock = 0;
        for (var p : fresh) {
            int s = p.getStock();
            if (s <= 0)        zeroStock++;
            else if (s <= 10)  lowStock++;
            else               inStock++;
        }

        var newPie = FXCollections.observableArrayList(
                new PieChart.Data("Stoklu", inStock),
                new PieChart.Data("Kritik (≤10)", lowStock),
                new PieChart.Data("Bitti (0)", zeroStock)
        );

        Platform.runLater(() -> {
            // tabloyu güncelle
            products.setAll(fresh);
            table.refresh();

            // grafiği güncelle (biriktirme YOK)
            pieData.setAll(newPie);
        });
    }

    private void startAutoRefresh() {
        exec.scheduleAtFixedRate(this::updateData, 1, 2, TimeUnit.SECONDS);
    }

    public void refresh() {
        updateData();
    }
}
