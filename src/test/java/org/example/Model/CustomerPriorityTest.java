package org.example.Model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

@DisplayName("Customer — öncelik puanı hesabı")
class CustomerPriorityTest {

    private static final double EPS = 1e-9;

    @Test
    void standardCustomerBaseScoreIsTen() {
        Customer c = new Customer(1, "Ada", 1000, "Standard");
        c.calculatePriorityScore();
        assertEquals(10.0, c.getPriorityScore(), EPS);
    }

    @Test
    void premiumCustomerBaseScoreIsFifteen() {
        Customer c = new Customer(2, "Bora", 1000, "Premium");
        c.calculatePriorityScore();
        assertEquals(15.0, c.getPriorityScore(), EPS);
    }

    @Test
    void waitingTimeAddsHalfPointEach() {
        Customer c = new Customer(3, "Can", 1000, "Standard");
        c.setWaitingTime(4);
        c.calculatePriorityScore();
        assertEquals(12.0, c.getPriorityScore(), EPS); // 10 + 4*0.5
    }

    @Test
    void incrementWaitingTimeBumpsCounterAndRecalculates() {
        Customer c = new Customer(4, "Derya", 1000, "Standard");
        c.incrementWaitingTime();
        assertEquals(1, c.getWaitingTime());
        assertEquals(10.5, c.getPriorityScore(), EPS); // 10 + 1*0.5
    }

    @Test
    void premiumIsCaseInsensitive() {
        Customer c = new Customer(5, "Efe", 1000, "premium");
        c.calculatePriorityScore();
        assertEquals(15.0, c.getPriorityScore(), EPS);
    }
}
