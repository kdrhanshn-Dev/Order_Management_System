package org.example.Service;

import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/** Katalog (ürün/müşteri) üzerinde global RW kilit. */
public final class CatalogRWLock {
    private static final ReentrantReadWriteLock RW =
            new ReentrantReadWriteLock(true); // fair: writer-starvation azaltır

    private CatalogRWLock() {}

    public static Lock readLock()  { return RW.readLock();  }
    public static Lock writeLock() { return RW.writeLock(); }
}
