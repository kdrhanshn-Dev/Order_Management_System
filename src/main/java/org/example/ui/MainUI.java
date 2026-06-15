package org.example.ui;

import javafx.application.Application;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.Scene;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.stage.Stage;

import org.example.Model.Customer;
import org.example.Repository.CustomerRepository;
import org.example.Repository.LogRepository;
import org.example.Repository.OrderRepository;
import org.example.Repository.ProductRepository;
import org.example.Service.AdminService;
import org.example.Service.CustomerService;
import org.example.util.DatabaseSeeder;

public class MainUI extends Application {

    @Override
    public void start(Stage stage) {

        // --- Repositories ---
        var customerRepo = new CustomerRepository();
        var productRepo  = new ProductRepository();
        var orderRepo    = new OrderRepository();
        var logRepo      = new LogRepository();

        // --- Services ---
        var customerService = new CustomerService(logRepo, productRepo, customerRepo, orderRepo);
        var adminService    = new AdminService(productRepo, logRepo);

        // (Eğer projende eski kullanım varsa:) AdminService runnable olduğu için bu thread sorun çıkarmaz.
        Thread adminThread = new Thread(adminService, "admin-thread");
        adminThread.setDaemon(true);
        adminThread.start();

        // --- Seed (yalnızca boşsa) ---
        new DatabaseSeeder(customerRepo, productRepo).seedAllIfEmpty();

        // --- Paylaşılan müşteri listesi (PriorityPanel için) ---
        ObservableList<Customer> sharedCustomers = FXCollections.observableArrayList(customerRepo.getAllCustomers());

        // --- Paneller ---
        var logPanel      = new LogPanel();
        var stockPanel    = new StockPanel(productRepo);
        var priorityPanel = new PriorityPanel(sharedCustomers);
        var customerPanel = new CustomerPanel(customerRepo, productRepo, orderRepo, customerService);
        var adminPanel    = new AdminPanel(productRepo, adminService);

        // --- UI Callback Kablolama ---
        // 1) Loglar:
        customerService.setUiLogger(logPanel.getLogConsumer());
        adminService.setUiLogger(logPanel.getLogConsumer());

        // 2) Refresh zinciri: sipariş/adminden sonra UI'ları yenile
        Runnable refreshAll = () -> {
            try {
                // PriorityPanel’in listesi sharedCustomers olduğundan setAll yapıyoruz
                sharedCustomers.setAll(customerRepo.getAllCustomers());
                // CustomerPanel ve StockPanel kendi verisini repo’dan çekiyor
                customerPanel.refresh();
                stockPanel.refresh();
            } catch (Exception ignore) { }
        };
        customerService.setUiRefresher(refreshAll);
        adminService.setUiRefresher(refreshAll);

        // --- Sekmeler ---
        TabPane tabs = new TabPane(
                new Tab("Öncelik",   priorityPanel),
                new Tab("Stok",      stockPanel),
                new Tab("Log",       logPanel),
                new Tab("Müşteri",   customerPanel),
                new Tab("Admin",     adminPanel)
        );
        tabs.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);

        // --- Sahne ---
        stage.setTitle("Sipariş ve Stok Yönetim Sistemi");
        stage.setScene(new Scene(tabs, 1000, 640));
        stage.show();

        // İlk açılışta bir kez yenile
        refreshAll.run();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
