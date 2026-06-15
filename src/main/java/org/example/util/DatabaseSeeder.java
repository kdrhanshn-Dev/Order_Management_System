// src/main/java/org/example/util/DatabaseSeeder.java
package org.example.util;

import org.example.Model.Customer;
import org.example.Model.Product;
import org.example.Repository.CustomerRepository;
import org.example.Repository.ProductRepository;

import java.util.Random;

public class DatabaseSeeder {

    private final CustomerRepository customerRepo;
    private final ProductRepository productRepo;
    private final Random rnd = new Random();

    public DatabaseSeeder(CustomerRepository customerRepo, ProductRepository productRepo) {
        this.customerRepo = customerRepo;
        this.productRepo  = productRepo;
    }

    /**
     * Eğer tablolar boşsa başlangıç verilerini yükler:
     * - Müşteriler: 5–10 arası, bütçe 500–3000 TL
     * - En az 2 Premium garanti
     * - Ürünler: 5 adet sabit
     */
    public void seedAllIfEmpty() {
        if (customerRepo.getAllCustomers().isEmpty()) {
            int randomCount = 5 + rnd.nextInt(6); // 5–10
            seedCustomers(randomCount);
        }
        if (productRepo.getAllProducts().isEmpty()) {
            seedProducts();
        }
    }

    private void seedCustomers(int count) {
        // Önce 2 Premium garanti
        for (int i = 1; i <= 2; i++) {
            double budget = 500 + rnd.nextInt(2501);
            Customer c = new Customer(
                    0,
                    "Premium-" + i,
                    budget,
                    "Premium"
            );
            customerRepo.addCustomer(c);
        }

        // Kalanlar random Premium/Standard
        for (int i = 3; i <= count; i++) {
            boolean premium = rnd.nextBoolean();
            String type = premium ? "Premium" : "Standard";
            double budget = 500 + rnd.nextInt(2501);

            Customer c = new Customer(
                    0,
                    type + "-" + i,
                    budget,
                    type
            );
            customerRepo.addCustomer(c);
        }
    }

    private void seedProducts() {
        productRepo.addProduct(new Product(0, "Product1", 500, 100));
        productRepo.addProduct(new Product(0, "Product2", 10,   50));
        productRepo.addProduct(new Product(0, "Product3", 200,  45));
        productRepo.addProduct(new Product(0, "Product4", 75,   75));
        productRepo.addProduct(new Product(0, "Product5", 0,   500));
    }
}