package org.example.Service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("OrderRequest — snapshot ve deadline mantığı")
class OrderRequestTest {

    @Test
    void deadlineIsCreatedAtPlusTimeout() {
        OrderRequest r = new OrderRequest(1, 2, 3, true, 1000);
        assertEquals(1000L, r.deadlineAt - r.createdAt);
    }

    @Test
    void snapshotFieldsArePreserved() {
        OrderRequest r = new OrderRequest(7, 8, 9, true, 500);
        assertEquals(7, r.customerId);
        assertEquals(8, r.productId);
        assertEquals(9, r.qty);
        assertTrue(r.isPremiumAtPlacement);
    }

    @Test
    void zeroTimeoutIsClampedToOneMillisecond() {
        OrderRequest r = new OrderRequest(1, 1, 1, false, 0);
        assertEquals(1L, r.deadlineAt - r.createdAt);
    }

    @Test
    void negativeTimeoutIsClampedToOneMillisecond() {
        OrderRequest r = new OrderRequest(1, 1, 1, false, -50);
        assertEquals(1L, r.deadlineAt - r.createdAt);
    }

    @Test
    void createdAtIsRecent() {
        long before = System.currentTimeMillis();
        OrderRequest r = new OrderRequest(1, 1, 1, false, 100);
        long after = System.currentTimeMillis();
        assertTrue(r.createdAt >= before && r.createdAt <= after);
    }
}
