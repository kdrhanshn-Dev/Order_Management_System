package org.example.Service;

/**
 * Sipariş talebi (snapshot + deadline)
 * - isPremiumAtPlacement: talep verildiği andaki premium durumu (snapshot)
 * - createdAt: talebin oluştuğu an
 * - deadlineAt: bu zaman damgasından sonra sipariş TIMEOUT sayılır
 */
public final class OrderRequest {
    public final int customerId;
    public final int productId;
    public final int qty;
    public final boolean isPremiumAtPlacement;
    public final long createdAt;
    public final long deadlineAt; // epoch millis

    public OrderRequest(int customerId, int productId, int qty,
                        boolean isPremiumAtPlacement, long timeoutMs) {
        this.customerId = customerId;
        this.productId = productId;
        this.qty = qty;
        this.isPremiumAtPlacement = isPremiumAtPlacement;
        this.createdAt = System.currentTimeMillis();
        this.deadlineAt = this.createdAt + Math.max(1, timeoutMs);
    }

    @Override
    public String toString() {
        return "OrderRequest{" +
                "customerId=" + customerId +
                ", productId=" + productId +
                ", qty=" + qty +
                ", premiumAtPlacement=" + isPremiumAtPlacement +
                ", createdAt=" + createdAt +
                ", deadlineAt=" + deadlineAt +
                '}';
    }
}
