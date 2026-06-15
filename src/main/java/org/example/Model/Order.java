package org.example.Model;

import javafx.beans.property.*;
import java.time.LocalDateTime;
import java.util.Locale;

/**
 * Order modeli (JavaFX Property'leri ile UI-uyumlu)
 * - Yeni: Enum tabanlı durum (OrderStatus)
 * - Geri uyumluluk: String get/setStatus() korunur (SUCCESS/FAILED eşlemesi dahil)
 */
public class Order {

    /** İsterlere uygun standart durumlar */
    public enum OrderStatus { PENDING, APPROVED, REJECTED, TIMEOUT, ERROR }

    private final IntegerProperty id         = new SimpleIntegerProperty(this, "id", 0);
    private final IntegerProperty customerId = new SimpleIntegerProperty(this, "customerId", 0);
    private final IntegerProperty productId  = new SimpleIntegerProperty(this, "productId", 0);
    private final IntegerProperty quantity   = new SimpleIntegerProperty(this, "quantity", 0);
    private final DoubleProperty  totalPrice = new SimpleDoubleProperty(this, "totalPrice", 0.0);

    // Enum tabanlı durum
    private final ObjectProperty<OrderStatus> status =
            new SimpleObjectProperty<>(this, "status", OrderStatus.PENDING);

    // Sipariş zamanı (repo doldurabilir; boşsa "şimdi")
    private final ObjectProperty<LocalDateTime> orderDate =
            new SimpleObjectProperty<>(this, "orderDate", LocalDateTime.now());

    // --- Yapıcılar ---

    public Order() {
        // Boş yapıcı (JavaFX / ORM uyumu için)
    }

    public Order(int id, int customerId, int productId, int quantity, double totalPrice, OrderStatus status) {
        setId(id);
        setCustomerId(customerId);
        setProductId(productId);
        setQuantity(quantity);
        setTotalPrice(totalPrice);
        setStatusEnum(status);
        setOrderDate(LocalDateTime.now());
    }

    public Order(int id, int customerId, int productId, int quantity, double totalPrice,
                 OrderStatus status, LocalDateTime orderDate) {
        setId(id);
        setCustomerId(customerId);
        setProductId(productId);
        setQuantity(quantity);
        setTotalPrice(totalPrice);
        setStatusEnum(status);
        setOrderDate(orderDate != null ? orderDate : LocalDateTime.now());
    }

    /** Geri uyumluluk: Durumu String veren eski çağrılar için */
    public Order(int id, int customerId, int productId, int quantity, double totalPrice, String status) {
        this(id, customerId, productId, quantity, totalPrice, mapStatus(status));
    }

    // --- Enum/String durum eşlemeleri (geri uyum için) ---

    /** Eski metin değerlerini enum'a dönüştürür; null/boş → PENDING */
    public static OrderStatus mapStatus(String s) {
        if (s == null) return OrderStatus.PENDING;
        String v = s.trim().toUpperCase(Locale.ROOT);
        switch (v) {
            case "SUCCESS":
            case "APPROVED":
                return OrderStatus.APPROVED;
            case "FAIL":
            case "FAILED":
            case "REJECTED":
                return OrderStatus.REJECTED;
            case "TIMEOUT":
                return OrderStatus.TIMEOUT;
            case "ERROR":
            case "DB_ERROR":
                return OrderStatus.ERROR;
            case "PENDING":
            default:
                return OrderStatus.PENDING;
        }
    }

    /** Enum durumunu verir (yeni kullanım) */
    public OrderStatus getStatusEnum() { return status.get(); }

    /** Enum durumunu ayarlar (yeni kullanım) */
    public void setStatusEnum(OrderStatus s) { this.status.set(s == null ? OrderStatus.PENDING : s); }

    /** JavaFX binding için enum property */
    public ObjectProperty<OrderStatus> statusProperty() { return status; }

    /** Geri uyumluluk: String durum döndürür (repo/ UI eski kodları kırılmasın) */
    public String getStatus() { return status.get().name(); }

    /** Geri uyumluluk: String durum ayarlar (SUCCESS/FAILED eşlemeli) */
    public void setStatus(String s) { setStatusEnum(mapStatus(s)); }

    // --- Diğer alanlar (standart JavaFX getter/setter + property) ---

    public int getId() { return id.get(); }
    public void setId(int value) { id.set(value); }
    public IntegerProperty idProperty() { return id; }

    public int getCustomerId() { return customerId.get(); }
    public void setCustomerId(int value) { customerId.set(value); }
    public IntegerProperty customerIdProperty() { return customerId; }

    public int getProductId() { return productId.get(); }
    public void setProductId(int value) { productId.set(value); }
    public IntegerProperty productIdProperty() { return productId; }

    public int getQuantity() { return quantity.get(); }
    public void setQuantity(int value) { quantity.set(value); }
    public IntegerProperty quantityProperty() { return quantity; }

    public double getTotalPrice() { return totalPrice.get(); }
    public void setTotalPrice(double value) { totalPrice.set(value); }
    public DoubleProperty totalPriceProperty() { return totalPrice; }

    public LocalDateTime getOrderDate() { return orderDate.get(); }
    public void setOrderDate(LocalDateTime dt) { orderDate.set(dt); }
    public ObjectProperty<LocalDateTime> orderDateProperty() { return orderDate; }

    @Override
    public String toString() {
        return "Order{" +
                "id=" + getId() +
                ", customerId=" + getCustomerId() +
                ", productId=" + getProductId() +
                ", quantity=" + getQuantity() +
                ", totalPrice=" + getTotalPrice() +
                ", status=" + getStatus() +
                ", orderDate=" + getOrderDate() +
                '}';
    }
}

