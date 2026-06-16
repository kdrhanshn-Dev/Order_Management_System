package org.example.Model;

import org.example.Model.Order.OrderStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

@DisplayName("Order.mapStatus — eski String durumlarını enum'a eşler")
class OrderStatusMappingTest {

    @Test
    void mapsSuccessAndApprovedToApproved() {
        assertEquals(OrderStatus.APPROVED, Order.mapStatus("SUCCESS"));
        assertEquals(OrderStatus.APPROVED, Order.mapStatus("APPROVED"));
    }

    @Test
    void mapsFailVariantsToRejected() {
        assertEquals(OrderStatus.REJECTED, Order.mapStatus("FAIL"));
        assertEquals(OrderStatus.REJECTED, Order.mapStatus("FAILED"));
        assertEquals(OrderStatus.REJECTED, Order.mapStatus("REJECTED"));
    }

    @Test
    void mapsErrorVariantsToError() {
        assertEquals(OrderStatus.ERROR, Order.mapStatus("ERROR"));
        assertEquals(OrderStatus.ERROR, Order.mapStatus("DB_ERROR"));
    }

    @Test
    void mapsTimeout() {
        assertEquals(OrderStatus.TIMEOUT, Order.mapStatus("TIMEOUT"));
    }

    @Test
    void isCaseInsensitiveAndTrimmed() {
        assertEquals(OrderStatus.APPROVED, Order.mapStatus("  success  "));
        assertEquals(OrderStatus.TIMEOUT, Order.mapStatus("timeout"));
    }

    @Test
    void nullAndUnknownFallBackToPending() {
        assertEquals(OrderStatus.PENDING, Order.mapStatus(null));
        assertEquals(OrderStatus.PENDING, Order.mapStatus("PENDING"));
        assertEquals(OrderStatus.PENDING, Order.mapStatus("something-else"));
    }

    @Test
    void stringConstructorUsesMapping() {
        Order o = new Order(1, 2, 3, 4, 100.0, "SUCCESS");
        assertEquals(OrderStatus.APPROVED, o.getStatusEnum());
    }
}
