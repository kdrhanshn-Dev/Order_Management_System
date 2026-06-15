package org.example.Service;

import javafx.application.Platform;
import org.example.Model.Customer;
import org.example.Model.Order;
import org.example.Model.Order.OrderStatus;
import org.example.Model.Product;
import org.example.Repository.CustomerRepository;
import org.example.Repository.DatabaseConnection;
import org.example.Repository.LogRepository;
import org.example.Repository.OrderRepository;
import org.example.Repository.ProductRepository;

import java.sql.Connection;
import java.util.ArrayList;
import java.util.Random;
import java.util.concurrent.Executors;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

/**
 * CustomerService — Adım 5:
 * - Öncelik kuyruğu snapshot comparator ile çalışır (DB'ye bakmaz).
 * - Her talep için deadline belirlenir; deadline aşılırsa TIMEOUT kaydı/log'u yazılır.
 * - Deadlock önleme: readLock → productLock → customerLock sırası.
 */
public class CustomerService {

    // --- TIMEOUT AYARLARI ---
    // Premium müşteriler için daha "sıkı" SLA (daha kısa timeout), Standard için daha uzun:
    private static final long TIMEOUT_MS_PREMIUM  = 4_000L; // 4 sn
    private static final long TIMEOUT_MS_STANDARD = 6_000L; // 6 sn

    // Repositories
    private final LogRepository logRepository;
    private final ProductRepository productRepository;
    private final CustomerRepository customerRepository;
    private final OrderRepository orderRepository;
    private final LocksRegistry locks = new LocksRegistry();

    // UI entegrasyonu (opsiyonel)
    private Consumer<String> uiLogger;
    private Runnable uiRefresher;

    // Öncelikli sipariş kuyruğu (snapshot comparator)
    private final PriorityBlockingQueue<OrderRequest> queue;

    // (Opsiyonel) arka plan görevleri
    private final ScheduledExecutorService scheduler =
            Executors.newScheduledThreadPool(2, r -> {
                Thread t = new Thread(r, "order-scheduler");
                t.setDaemon(true);
                return t;
            });
    private final Random random = new Random();

    public CustomerService(LogRepository logRepository,
                           ProductRepository productRepository,
                           CustomerRepository customerRepository,
                           OrderRepository orderRepository) {
        this.logRepository = logRepository;
        this.productRepository = productRepository;
        this.customerRepository = customerRepository;
        this.orderRepository = orderRepository;

        // === Comparator: yalnızca snapshot alanları ===
        this.queue = new PriorityBlockingQueue<>(64, (a, b) -> {
            double baseA = a.isPremiumAtPlacement ? 15 : 10;
            double baseB = b.isPremiumAtPlacement ? 15 : 10;
            long now = System.currentTimeMillis();
            double sA = baseA + ((now - a.createdAt) / 1000.0) * 0.5;
            double sB = baseB + ((now - b.createdAt) / 1000.0) * 0.5;
            int cmp = Double.compare(sB, sA);                 // büyük skor önce
            return (cmp != 0) ? cmp : Long.compare(a.createdAt, b.createdAt); // eşitse FIFO
        });

        // Tüketici iş parçacığı
        scheduler.execute(this::consumeLoop);

        // Bekleme süresi etkisi için periyodik re-heap
        scheduler.scheduleAtFixedRate(this::reheapByWaiting, 1, 1, TimeUnit.SECONDS);
    }

    // --- UI entegrasyonu ---
    public void setUiLogger(Consumer<String> uiLogger) { this.uiLogger = uiLogger; }
    public void setUiRefresher(Runnable uiRefresher)   { this.uiRefresher = uiRefresher; }
    private void logToUI(String msg) { if (uiLogger != null) Platform.runLater(() -> uiLogger.accept(msg)); }
    private void refreshUI()         { if (uiRefresher != null) Platform.runLater(uiRefresher); }

    // === Sipariş ekleme (UI çağırır) ===
    public synchronized void addOrder(Customer customer, Product product, int qty) {
        double totalCost = product.getPrice() * qty;

        // 1) Adet kontrolü (1–5)
        if (qty < 1 || qty > 5) {
            logRepository.createLog("Hata", customer.getId(),
                    "❌ " + customer.getType() + " / " + customer.getName() +
                            " → " + product.getName() + " x" + qty + " | Sipariş 1–5 adet olmalı.");
            logToUI("❌ " + customer.getName() + " için sipariş adedi 1–5 olmalı.");
            return;
        }

        // 2) Hızlı ön-kontrol (bilgi amaçlı; asıl kontrol consumer’da atomik)
        if (customer.getBudget() < totalCost) {
            logRepository.createLog("Hata", customer.getId(),
                    "❌ " + customer.getType() + " / " + customer.getName() +
                            " → " + product.getName() + " x" + qty +
                            " | Toplam: " + totalCost + " TL | Bütçe yetersiz (ön-kontrol).");
            logToUI("❌ " + customer.getName() + " için bütçe yetersiz (ön-kontrol).");
            return;
        }
        if (product.getStock() < qty) {
            logRepository.createLog("Hata", customer.getId(),
                    "❌ " + customer.getType() + " / " + customer.getName() +
                            " → " + product.getName() + " x" + qty +
                            " | İstenen: " + qty + ", Mevcut: " + product.getStock() + " (ön-kontrol).");
            logToUI("❌ " + product.getName() + " stoğu yetersiz (ön-kontrol).");
            return;
        }

        // 3) Snapshot + deadline
        boolean premiumNow = "Premium".equalsIgnoreCase(customer.getType());
        long timeoutMs = premiumNow ? TIMEOUT_MS_PREMIUM : TIMEOUT_MS_STANDARD;
        queue.offer(new OrderRequest(customer.getId(), product.getId(), qty, premiumNow, timeoutMs));

        logRepository.createLog("Bilgi", customer.getId(),
                "📦 Sipariş sıraya alındı: " + customer.getType() + " / " + customer.getName() +
                        " → " + product.getName() + " x" + qty + " | Toplam: " + totalCost + " TL");
        logToUI("⏳ " + customer.getName() + " → " + product.getName() + " x" + qty + " sıraya alındı.");
        refreshUI();
    }

    // === Tüketici döngüsü ===
    private void consumeLoop() {
        try {
            while (true) {
                OrderRequest r = queue.take(); // en yüksek öncelikli istek

                Customer cust = customerRepository.getAllCustomers().stream()
                        .filter(c -> c.getId() == r.customerId).findFirst().orElse(null);
                Product  prod = productRepository.getProductById(r.productId);

                if (cust == null || prod == null) {
                    logToUI("❌ Geçersiz istek (müşteri/ürün bulunamadı).");
                    continue;
                }

                double total = prod.getPrice() * r.qty;

                // Deadline'a bak: Kuyrukta beklerken süresi geçmiş olabilir
                if (System.currentTimeMillis() > r.deadlineAt) {
                    recordTimeout(cust.getId(), prod.getId(), r.qty, total, "kuyruk");
                    continue;
                }

                // --- PAYLAŞILAN KİLİTLER ---
                var rl    = CatalogRWLock.readLock();          // global readLock
                var pLock = locks.productLock(r.productId);
                var cLock = locks.customerLock(r.customerId);

                // Kilit sırası: readLock → productLock → customerLock
                rl.lock();
                pLock.lock();
                cLock.lock();
                try (Connection conn = DatabaseConnection.getConnection()) {
                    conn.setAutoCommit(false);

                    // 1) Stok azalt (koşullu)
                    boolean stockOk = productRepository.decStockIfEnough(conn, r.productId, r.qty);
                    if (!stockOk) {
                        orderRepository.createOrder(conn,
                                new Order(0, cust.getId(), prod.getId(), r.qty, total, OrderStatus.REJECTED));
                        logRepository.createLog("Hata", cust.getId(), "Yetersiz stok (atomik kontrol).");
                        conn.commit();      // log + rejected sipariş kalıcı
                        logToUI("❌ " + cust.getName() + " → stok yetersiz (atomik).");
                        refreshUI();
                        continue;
                    }

                    // Deadline kontrolü (stoktan sonra)
                    if (System.currentTimeMillis() > r.deadlineAt) {
                        conn.rollback(); // stok geri döner
                        recordTimeout(cust.getId(), prod.getId(), r.qty, total, "stoktan sonra");
                        continue;
                    }

                    // 2) Bütçe düş (koşullu)
                    boolean budgetOk = customerRepository.debitIfEnough(conn, r.customerId, total);
                    if (!budgetOk) {
                        // Telafi: stoğu geri al, REJECTED sipariş + log
                        productRepository.incStock(conn, r.productId, r.qty);
                        orderRepository.createOrder(conn,
                                new Order(0, cust.getId(), prod.getId(), r.qty, total, OrderStatus.REJECTED));
                        logRepository.createLog("Hata", cust.getId(), "Bütçe yetersiz (atomik kontrol).");
                        conn.commit();
                        logToUI("❌ " + cust.getName() + " → bütçe yetersiz (atomik).");
                        refreshUI();
                        continue;
                    }

                    // Deadline kontrolü (bütçeden sonra)
                    if (System.currentTimeMillis() > r.deadlineAt) {
                        conn.rollback(); // hem stok hem bütçe geri döner
                        recordTimeout(cust.getId(), prod.getId(), r.qty, total, "bütçeden sonra");
                        continue;
                    }

                    // 3) Başarılı sipariş + log
                    orderRepository.createOrder(conn,
                            new Order(0, cust.getId(), prod.getId(), r.qty, total, OrderStatus.APPROVED));
                    logRepository.createLog("Bilgi", cust.getId(),
                            "✅ [" + cust.getType() + "] " + cust.getName() +
                                    " → " + prod.getName() + " x" + r.qty +
                                    " | Toplam: " + total + " TL | Status: APPROVED");

                    // Commit'ten hemen önce son kontrol (çok nadir gereken bir güvenlik ağı)
                    if (System.currentTimeMillis() > r.deadlineAt) {
                        conn.rollback();
                        recordTimeout(cust.getId(), prod.getId(), r.qty, total, "commit öncesi");
                        continue;
                    }

                    // Kalıcılaştır
                    conn.commit();

                    // --- UI model eşitle (commit sonrası) ---
                    prod.setStock(prod.getStock() - r.qty);
                    cust.setBudget(cust.getBudget() - total);
                    cust.setTotalSpent(cust.getTotalSpent() + total);

                    logToUI("✅ " + cust.getName() + " siparişi işlendi: " + prod.getName());
                    refreshUI();

                } catch (Exception ex) {
                    logRepository.createLog("Hata", cust.getId(), "İşlem hatası: " + ex.getMessage());
                    logToUI("❌ " + cust.getName() + " siparişi hata verdi.");
                    refreshUI();
                } finally {
                    cLock.unlock();
                    pLock.unlock();
                    rl.unlock(); // en son bırak
                }
            }
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
        } catch (Exception e) {
            logToUI("❌ Tüketici döngüsü hatası: " + e.getMessage());
        }
    }

    // TIMEOUT kayıt/log (bağımsız bağlantıyla; önceki transaction rollback edilmiş olur)
    private void recordTimeout(int customerId, int productId, int qty, double total, String phase) {
        try {
            orderRepository.createOrderStandalone(
                    new Order(0, customerId, productId, qty, total, OrderStatus.TIMEOUT));
        } catch (Exception ignore) { /* best-effort */ }
        try {
            logRepository.createLog("Uyarı", customerId,
                    "⏳ TIMEOUT (" + phase + "): productId=" + productId + " x" + qty + " toplam=" + total + " TL");
        } catch (Exception ignore) { }
        logToUI("⏳ Sipariş TIMEOUT oldu (" + phase + ").");
        refreshUI();
    }

    // === Dinamik öncelik için periyodik re-heap ===
    private void reheapByWaiting() {
        try {
            ArrayList<OrderRequest> tmp = new ArrayList<>(queue.size());
            queue.drainTo(tmp);
            for (OrderRequest r : tmp) queue.offer(r);
            refreshUI();
        } catch (Exception ignore) { }
    }
}
