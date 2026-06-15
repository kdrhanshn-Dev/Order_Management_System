package org.example.Service;

import javafx.application.Platform;
import org.example.Model.Product;
import org.example.Repository.LogRepository;
import org.example.Repository.ProductRepository;

import java.util.Objects;
import java.util.concurrent.locks.Lock;
import java.util.function.Consumer;

/**
 * AdminService — Adım 3:
 * - Katalog yazma işlemlerinde global writeLock kullanır.
 * - Ürün bazlı işlemlerde ayrıca productLock alır.
 * - UI log ve refresh callback'leri opsiyoneldir.
 * - Geri uyumluluk: updateStock(...) alias'ları eklendi.
 * - Runnable: new Thread(adminService, "admin-thread") ile uyumlu.
 */
public class AdminService implements Runnable {

    private final ProductRepository productRepository;
    private final LogRepository logRepository;
    private final LocksRegistry locks = new LocksRegistry();

    // UI entegrasyonu (opsiyonel)
    private Consumer<String> uiLogger;
    private Runnable uiRefresher;

    public AdminService(ProductRepository productRepository, LogRepository logRepository) {
        this.productRepository = Objects.requireNonNull(productRepository);
        this.logRepository = Objects.requireNonNull(logRepository);
    }

    // Eğer projede arka plan bir iş gerekmiyorsa bu metodu boş bırakmak yeterli.
    @Override
    public void run() {
        // no-op (arka plan döngünüz yoksa boş)
        // İsterseniz burada periyodik bakım işleri koşturabilirsiniz.
    }

    // --- UI callback setter'ları ---
    public void setUiLogger(Consumer<String> uiLogger) { this.uiLogger = uiLogger; }
    public void setUiRefresher(Runnable uiRefresher)   { this.uiRefresher = uiRefresher; }
    private void logToUI(String msg) {
        if (uiLogger != null) Platform.runLater(() -> uiLogger.accept(msg));
    }
    private void refreshUI() {
        if (uiRefresher != null) Platform.runLater(uiRefresher);
    }

    // ================== İşlemler ==================

    /** Yeni ürün ekler (isim benzersiz olmalı). Müşteriler writeLock sırasında bekler. */
    public void addProduct(String name, int stock, double price) {
        if (name == null || name.isBlank()) {
            logRepository.createLog("Hata", 0, "Ürün adı boş olamaz.");
            logToUI("❌ Ürün adı boş olamaz.");
            return;
        }
        if (stock < 0) {
            logRepository.createLog("Hata", 0, "Stok negatif olamaz: " + stock);
            logToUI("❌ Stok negatif olamaz.");
            return;
        }
        if (price < 0) {
            logRepository.createLog("Hata", 0, "Fiyat negatif olamaz: " + price);
            logToUI("❌ Fiyat negatif olamaz.");
            return;
        }

        Lock wl = CatalogRWLock.writeLock();
        wl.lock();
        try {
            if (productRepository.existsByName(name)) {
                logRepository.createLog("Uyarı", 0, "Ürün zaten var: " + name);
                logToUI("⚠️ Ürün zaten var: " + name);
                return;
            }
            productRepository.addProduct(new Product(0, name, stock, price));
            logRepository.createLog("Bilgi", 0,
                    "➕ Ürün eklendi: " + name + " (stok=" + stock + ", fiyat=" + price + ")");
            logToUI("➕ Ürün eklendi: " + name);
            refreshUI();
        } catch (Exception e) {
            logRepository.createLog("Hata", 0, "Ürün ekleme hatası: " + e.getMessage());
            logToUI("❌ Ürün ekleme hatası: " + e.getMessage());
        } finally {
            wl.unlock();
        }
    }

    /** Ürün stokunu günceller. Aynı üründe admin yarışını önlemek için productLock da alınır. */
    public void updateProductStock(int productId, int newStock) {
        if (newStock < 0) {
            logRepository.createLog("Hata", 0, "Stok negatif olamaz: " + newStock);
            logToUI("❌ Stok negatif olamaz.");
            return;
        }

        Lock wl = CatalogRWLock.writeLock();
        wl.lock();
        var pLock = locks.productLock(productId);
        pLock.lock();
        try {
            var p = productRepository.getProductById(productId);
            if (p == null) {
                logRepository.createLog("Uyarı", 0, "Ürün bulunamadı: id=" + productId);
                logToUI("⚠️ Ürün bulunamadı: id=" + productId);
                return;
            }

            productRepository.updateProductStock(productId, newStock);
            logRepository.createLog("Bilgi", 0,
                    "✏️ Stok güncellendi: id=" + productId + " (" + p.getName() + ") → " + newStock);
            logToUI("✏️ Stok güncellendi: " + p.getName() + " → " + newStock);
            refreshUI();
        } catch (Exception e) {
            logRepository.createLog("Hata", 0, "Stok güncelleme hatası: " + e.getMessage());
            logToUI("❌ Stok güncelleme hatası: " + e.getMessage());
        } finally {
            pLock.unlock();
            wl.unlock();
        }
    }

    /** Ürünü siler. (FK kısıtları varsa DB tarafı hata verebilir; mesajlanır.) */
    public void deleteProduct(int productId) {
        Lock wl = CatalogRWLock.writeLock();
        wl.lock();
        var pLock = locks.productLock(productId);
        pLock.lock();
        try {
            var p = productRepository.getProductById(productId);
            if (p == null) {
                logRepository.createLog("Uyarı", 0, "Silinecek ürün bulunamadı: id=" + productId);
                logToUI("⚠️ Silinecek ürün bulunamadı: id=" + productId);
                return;
            }

            productRepository.deleteProduct(productId);
            logRepository.createLog("Bilgi", 0, "🗑️ Ürün silindi: id=" + productId + " (" + p.getName() + ")");
            logToUI("🗑️ Ürün silindi: " + p.getName());
            refreshUI();
        } catch (Exception e) {
            logRepository.createLog("Hata", 0, "Ürün silme hatası: " + e.getMessage());
            logToUI("❌ Ürün silme hatası: " + e.getMessage());
        } finally {
            pLock.unlock();
            wl.unlock();
        }
    }

    // ================== Geri uyumluluk alias'ları ==================

    /** Eski UI çağrıları için alias: updateStock(productId, newStock) → updateProductStock(...) */
    public void updateStock(int productId, int newStock) {
        updateProductStock(productId, newStock);
    }

    /** Eski UI çağrıları için alias: updateStock(product, newStock) → updateProductStock(product.getId(), ...) */
    public void updateStock(Product product, int newStock) {
        if (product == null) {
            logRepository.createLog("Hata", 0, "updateStock: product null.");
            logToUI("❌ updateStock: product null.");
            return;
        }
        updateProductStock(product.getId(), newStock);
    }
}
