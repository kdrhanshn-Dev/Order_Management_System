package org.example.Service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.concurrent.locks.Lock;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("CatalogRWLock — okuma/yazma kilidi anlamı")
class CatalogRWLockTest {

    @Test
    void locksAreNotNull() {
        assertNotNull(CatalogRWLock.readLock());
        assertNotNull(CatalogRWLock.writeLock());
    }

    @Test
    void multipleReadersCanHoldTheLock() {
        Lock r = CatalogRWLock.readLock();
        r.lock();
        try {
            // Aynı thread içinde ikinci bir okuma kilidi de alınabilmeli (paylaşımlı)
            assertTrue(r.tryLock());
            r.unlock();
        } finally {
            r.unlock();
        }
    }

    @Test
    void writeLockIsBlockedWhileReadHeld() throws InterruptedException {
        Lock r = CatalogRWLock.readLock();
        r.lock();
        try {
            final boolean[] acquired = {true};
            Thread writer = new Thread(() -> acquired[0] = CatalogRWLock.writeLock().tryLock());
            writer.start();
            writer.join();
            assertFalse(acquired[0], "Okuma kilidi tutulurken yazma kilidi alınamamalı");
        } finally {
            r.unlock();
        }
    }
}
