package org.example.ui;

import javafx.geometry.Insets;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import org.example.Model.Product;
import org.example.Repository.ProductRepository;
import org.example.Service.AdminService;

public class AdminPanel extends VBox {

    private final ObservableList<Product> products;
    private final TableView<Product> productTable;
    private final ProductRepository productRepo;
    private final AdminService adminService;

    // UI bileşenleri
    private final TextField nameField       = new TextField();
    private final TextField stockField      = new TextField();
    private final TextField priceField      = new TextField();
    private final Button    addBtn          = new Button("Ürün Ekle");

    private final Button    deleteBtn       = new Button("Seçili Ürünü Sil");
    private final TextField stockUpdateField= new TextField();
    private final Button    updateBtn       = new Button("Stok Güncelle");

    // Overlay
    private final BusyOverlay overlay = new BusyOverlay();

    public AdminPanel(ProductRepository productRepo, AdminService adminService) {
        this.productRepo   = productRepo;
        this.adminService  = adminService;

        this.products     = FXCollections.observableArrayList(productRepo.getAllProducts());
        this.productTable = new TableView<>(products);

        setSpacing(10);
        setPadding(new Insets(10));

        /* ===== Tablo ===== */
        TableColumn<Product, Number> idCol = new TableColumn<>("ID");
        idCol.setCellValueFactory(data -> data.getValue().idProperty());
        idCol.setPrefWidth(60);

        TableColumn<Product, String> nameCol = new TableColumn<>("Ürün");
        nameCol.setCellValueFactory(data -> data.getValue().nameProperty());
        nameCol.setPrefWidth(220);

        TableColumn<Product, Number> stockCol = new TableColumn<>("Stok");
        stockCol.setCellValueFactory(data -> data.getValue().stockProperty());
        stockCol.setPrefWidth(120);

        TableColumn<Product, Number> priceCol = new TableColumn<>("Fiyat");
        priceCol.setCellValueFactory(data -> data.getValue().priceProperty());
        priceCol.setPrefWidth(140);

        productTable.getColumns().addAll(idCol, nameCol, stockCol, priceCol);
        productTable.setPrefHeight(360);

        /* ===== Formlar ===== */
        nameField.setPromptText("Ürün Adı");

        stockField.setPromptText("Stok");
        stockField.setTextFormatter(UiFX.integerFormatter(0, Integer.MAX_VALUE));

        priceField.setPromptText("Fiyat");
        priceField.setTextFormatter(UiFX.doubleFormatter(0.0));

        addBtn.setOnAction(e -> onAddProduct());

        HBox addForm = new HBox(10, nameField, stockField, priceField, addBtn);

        deleteBtn.setOnAction(e -> onDeleteProduct());

        stockUpdateField.setPromptText("Yeni Stok");
        stockUpdateField.setTextFormatter(UiFX.integerFormatter(0, Integer.MAX_VALUE));

        updateBtn.setOnAction(e -> onUpdateStock());

        HBox updateForm = new HBox(10, stockUpdateField, updateBtn);

        // İçeriği tek bir VBox'ta topla ve StackPane ile overlay üstüne koy
        VBox content = new VBox(10, productTable, addForm, deleteBtn, updateForm);
        Parent root   = new StackPane(content, overlay);

        getChildren().setAll(root);
    }

    /* ================== Event Handlers ================== */

    private void onAddProduct() {
        String name = nameField.getText() == null ? "" : nameField.getText().trim();
        int    stock = UiFX.parseIntOr(stockField, -1);
        double price = UiFX.parseDoubleOr(priceField, -1);

        overlay.show();
        disableAll(true);

        UiFX.runAsync(
                () -> adminService.addProduct(name, stock, price),
                () -> {
                    refreshProducts();
                    nameField.clear();
                    stockField.clear();
                    priceField.clear();
                    showAlert("Başarılı", "Yeni ürün eklendi!");
                    overlay.hide();
                    disableAll(false);
                },
                ex -> {
                    showAlert("Hata", "Ürün eklenirken hata: " + ex.getMessage());
                    overlay.hide();
                    disableAll(false);
                }
        );
    }

    private void onDeleteProduct() {
        Product selected = productTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showAlert("Uyarı", "Lütfen silmek için ürün seçin.");
            return;
        }
        overlay.show();
        disableAll(true);
        UiFX.runAsync(
                () -> adminService.deleteProduct(selected.getId()),
                () -> {
                    refreshProducts();
                    showAlert("Başarılı", "Ürün silindi!");
                    overlay.hide();
                    disableAll(false);
                },
                ex -> {
                    showAlert("Hata", "Ürün silinemedi: " + ex.getMessage());
                    overlay.hide();
                    disableAll(false);
                }
        );
    }

    private void onUpdateStock() {
        Product selected = productTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showAlert("Uyarı", "Lütfen güncellenecek ürünü seçin.");
            return;
        }
        int newStock = UiFX.parseIntOr(stockUpdateField, -1);
        overlay.show();
        disableAll(true);
        UiFX.runAsync(
                () -> adminService.updateStock(selected.getId(), newStock),
                () -> {
                    refreshProducts();
                    showAlert("Başarılı", "Stok güncellendi!");
                    overlay.hide();
                    disableAll(false);
                },
                ex -> {
                    showAlert("Hata", "Stok güncellenemedi: " + ex.getMessage());
                    overlay.hide();
                    disableAll(false);
                }
        );
    }

    /* ================== Helpers ================== */

    private void disableAll(boolean b) {
        addBtn.setDisable(b);
        deleteBtn.setDisable(b);
        updateBtn.setDisable(b);
        nameField.setDisable(b);
        stockField.setDisable(b);
        priceField.setDisable(b);
        stockUpdateField.setDisable(b);
        productTable.setDisable(b);
    }

    private void refreshProducts() {
        products.setAll(productRepo.getAllProducts());
        productTable.refresh();
    }

    private void showAlert(String title, String msg) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(msg);
        alert.showAndWait();
    }
}
