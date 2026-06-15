package org.example.Service;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;

public class LocksRegistry {
    private final ConcurrentHashMap<Integer, ReentrantLock> productLocks = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<Integer, ReentrantLock> customerLocks = new ConcurrentHashMap<>();

    public ReentrantLock productLock(int productId) {
        return productLocks.computeIfAbsent(productId, id -> new ReentrantLock(true));
    }

    public ReentrantLock customerLock(int customerId) {
        return customerLocks.computeIfAbsent(customerId, id -> new ReentrantLock(true));
    }
}

