package org.example.ui;

import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.util.StringConverter;

import org.example.Model.Customer;
import org.example.Model.Product;
import org.example.Repository.CustomerRepository;
import org.example.Repository.OrderRepository;
import org.example.Repository.ProductRepository;
import org.example.Service.CustomerService;

import java.text.NumberFormat;
import java.util.Locale;

public class CustomerPanel extends VBox {

    private final CustomerRepository customerRepo;
    private final ProductRepository productRepo;
    @SuppressWarnings("unused")
    private final OrderRepository orderRepo; // ileride lazım olabilir
    private final CustomerService customerService;

    private final ObservableList<Customer> customers = FXCollections.observableArrayList();
    private final ObservableList<Product>  products  = FXCollections.observableArrayList();

    // UI
    private final TableView<Customer> customerTable = new TableView<>();
    private final ComboBox<Product>   productCombo  = new ComboBox<>();
    private final TextField           qtyField      = new TextField();
    private final Button              orderBtn      = new Button("➕ Sipariş Ver");
    private final Button              btnRefresh    = new Button("↻ Yenile");
    private final Button              btnHealth     = new Button("🩺 DB Kontrol");

    private final BusyOverlay overlay = new BusyOverlay();

    private final NumberFormat trTL = NumberFormat.getCurrencyInstance(new Locale("tr","TR"));

    public CustomerPanel(CustomerRepository customerRepo,
                         ProductRepository productRepo,
                         OrderRepository orderRepo,
                         CustomerService customerService) {

        this.customerRepo    = customerRepo;
        this.productRepo     = productRepo;
        this.orderRepo       = orderRepo;
        this.customerService = customerService;

        setSpacing(10);
        setPadding(new Insets(10));

        /* ===== Üst araç çubuğu ===== */
        btnRefresh.setOnAction(e -> refresh());
        btnHealth.setOnAction(e -> showHealth());
        HBox toolbar = new HBox(10, btnRefresh, btnHealth);

        /* ===== Tablo ===== */
        customerTable.setItems(customers);
        customerTable.getSelectionModel().setSelectionMode(SelectionMode.SINGLE);
        customerTable.setPlaceholder(new Label("Müşteri yok — DB’de Customers boş olabilir."));

        TableColumn<Customer, Number> idCol = new TableColumn<>("ID");
        idCol.setCellValueFactory(d -> d.getValue().idProperty());
        idCol.setPrefWidth(60);

        TableColumn<Customer, String> nameCol = new TableColumn<>("Ad");
        nameCol.setCellValueFactory(d -> d.getValue().nameProperty());
        nameCol.setPrefWidth(180);

        TableColumn<Customer, Number> budgetCol = new TableColumn<>("Bütçe");
        budgetCol.setCellValueFactory(d -> d.getValue().budgetProperty());
        budgetCol.setPrefWidth(120);
        budgetCol.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(Number v, boolean empty) {
                super.updateItem(v, empty);
                if (empty || v == null) { setText(null); setStyle(""); return; }
                setText(trTL.format(v.doubleValue()));
                setStyle("-fx-alignment: CENTER-RIGHT;"
                        + (v.doubleValue() <= 50 ? "-fx-text-fill:#dc2626;" : ""));
            }
        });

        TableColumn<Customer, String> typeCol = new TableColumn<>("Tür");
        typeCol.setCellValueFactory(d -> d.getValue().typeProperty());
        typeCol.setPrefWidth(110);
        typeCol.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(String s, boolean empty) {
                super.updateItem(s, empty);
                if (empty || s == null) { setText(null); setGraphic(null); return; }
                String icon = "Premium".equalsIgnoreCase(s) ? "👑 " : "👤 ";
                Label chip = new Label(icon + s);
                chip.getStyleClass().addAll("chip",
                        "Premium".equalsIgnoreCase(s) ? "chip-success" : "chip-idle");
                chip.setMouseTransparent(true);
                setGraphic(chip);
                setText(null);
                setStyle("-fx-alignment: CENTER;");
            }
        });

        TableColumn<Customer, String> statusCol = new TableColumn<>("Durum");
        statusCol.setCellValueFactory(d -> d.getValue().orderStatusProperty());
        statusCol.setPrefWidth(130);
        statusCol.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(String s, boolean empty) {
                super.updateItem(s, empty);
                if (empty || s == null) { setText(null); setGraphic(null); return; }
                String cls = switch (s.toUpperCase()) {
                    case "SUCCESS", "APPROVED" -> "chip-success";
                    case "FAIL", "REJECTED"    -> "chip-error";
                    case "TIMEOUT"             -> "chip-warn";
                    default                    -> "chip-idle";
                };
                Label chip = new Label(s);
                chip.getStyleClass().addAll("chip", cls);
                chip.setMouseTransparent(true);
                setGraphic(chip);
                setText(null);
                setStyle("-fx-alignment: CENTER;");
            }
        });

        TableColumn<Customer, Number> spentCol = new TableColumn<>("Harcanan");
        spentCol.setCellValueFactory(d -> d.getValue().totalSpentProperty());
        spentCol.setPrefWidth(120);
        spentCol.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(Number v, boolean empty) {
                super.updateItem(v, empty);
                if (empty || v == null) { setText(null); return; }
                setText(trTL.format(v.doubleValue()));
                setStyle("-fx-alignment: CENTER-RIGHT;");
            }
        });

        customerTable.getColumns().addAll(idCol, nameCol, budgetCol, typeCol, statusCol, spentCol);
        customerTable.setPrefHeight(360);

        /* ===== Sipariş Formu ===== */
        productCombo.setItems(products);
        productCombo.setPromptText("Ürün Seç");
        productCombo.setPrefWidth(260);

        productCombo.setCellFactory(list -> new ListCell<>() {
            @Override protected void updateItem(Product p, boolean empty) {
                super.updateItem(p, empty);
                if (empty || p == null) { setText(null); return; }
                setText(p.getName() + " (Stok: " + p.getStock() + ") — " + trTL.format(p.getPrice()));
            }
        });
        productCombo.setButtonCell(new ListCell<>() {
            @Override protected void updateItem(Product p, boolean empty) {
                super.updateItem(p, empty);
                if (empty || p == null) { setText(null); return; }
                setText(p.getName() + " (Stok: " + p.getStock() + ") — " + trTL.format(p.getPrice()));
            }
        });
        productCombo.setConverter(new StringConverter<>() {
            @Override public String toString(Product p) { return p == null ? "" : p.getName(); }
            @Override public Product fromString(String s) { return null; }
        });

        qtyField.setPromptText("Adet (1–5)");
        qtyField.setPrefWidth(100);
        qtyField.setTextFormatter(UiFX.integerFormatter(1, 5));

        orderBtn.setOnAction(e -> placeOrder());

        HBox orderForm = new HBox(10, productCombo, qtyField, orderBtn);

        VBox content = new VBox(10, toolbar, customerTable, orderForm);
        getChildren().setAll(new StackPane(content, overlay));

        // İlk açılış
        refresh();
    }

    private void placeOrder() {
        Customer selectedCustomer = customerTable.getSelectionModel().getSelectedItem();
        Product  selectedProduct  = productCombo.getSelectionModel().getSelectedItem();
        if (selectedCustomer == null || selectedProduct == null) {
            showAlert("Hata", "Müşteri ve ürün seçiniz!");
            return;
        }
        int qty = UiFX.parseIntOr(qtyField, 1);

        orderBtn.setDisable(true);
        overlay.show();
        UiFX.runAsync(
                () -> customerService.addOrder(selectedCustomer, selectedProduct, qty),
                () -> {
                    // CustomerService zaten UI’ı tazeliyor; burada hafif temizlik yapalım
                    qtyField.clear();
                    orderBtn.setDisable(false);
                    overlay.hide();
                },
                ex -> {
                    showAlert("Hata", "Sipariş verilemedi: " + ex.getMessage());
                    orderBtn.setDisable(false);
                    overlay.hide();
                }
        );
    }

    /** Veriyi yenile + mevcut seçimleri ID ile koru */
    public void refresh() {
        try {
            // Mevcut seçimleri hatırla
            Integer selCustomerId = null;
            Customer selCustomer = customerTable.getSelectionModel().getSelectedItem();
            if (selCustomer != null) selCustomerId = selCustomer.getId();

            Integer selProductId = null;
            Product selProduct = productCombo.getSelectionModel().getSelectedItem();
            if (selProduct != null) selProductId = selProduct.getId();

            var cs = customerRepo.getAllCustomers();
            var ps = productRepo.getAllProducts();

            customers.setAll(cs);
            products.setAll(ps);

            // Müşteri re-select
            if (!customers.isEmpty()) {
                if (selCustomerId != null) {
                    int idx = -1;
                    for (int i = 0; i < customers.size(); i++) {
                        if (customers.get(i).getId() == selCustomerId) { idx = i; break; }
                    }
                    if (idx >= 0) customerTable.getSelectionModel().select(idx);
                    else          customerTable.getSelectionModel().selectFirst();
                } else {
                    customerTable.getSelectionModel().selectFirst();
                }
            } else {
                customerTable.getSelectionModel().clearSelection();
            }

            // Ürün re-select
            if (!products.isEmpty()) {
                Product match = null;
                if (selProductId != null) {
                    for (Product p : products) if (p.getId() == selProductId) { match = p; break; }
                }
                productCombo.getSelectionModel().select(match != null ? match : products.get(0));
            } else {
                productCombo.getSelectionModel().clearSelection();
            }

            customerTable.refresh();

            if (customers.isEmpty() || products.isEmpty()) {
                showAlert("Uyarı",
                        "Listeler boş görünüyor.\nCustomers: " + customers.size() +
                                " | Products: " + products.size() +
                                "\nDB bağlantısı/şema/veri durumunu kontrol edin.");
            }
        } catch (Exception ex) {
            showAlert("DB Hatası", "Veriler yüklenirken hata: " + ex.getMessage());
        }
    }

    /** Basit sağlık raporu */
    private void showHealth() {
        try {
            int c = customerRepo.getAllCustomers().size();
            int p = productRepo.getAllProducts().size();
            showAlert("DB Sağlık", "Customers: " + c + "\nProducts: " + p);
        } catch (Exception ex) {
            showAlert("DB Hatası", "Sağlık kontrolü başarısız: " + ex.getMessage());
        }
    }

    private void showAlert(String title, String msg) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(msg);
        alert.showAndWait();
    }
}
