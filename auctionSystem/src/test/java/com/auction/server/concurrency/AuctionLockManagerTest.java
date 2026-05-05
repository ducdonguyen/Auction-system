package com.auction.server.concurrency;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

class AuctionLockManagerTest {

    @Test
    @DisplayName("Kiểm thử lockAndRun thực thi hành động")
    void testLockAndRunExecutesAction() {
        AuctionLockManager manager = new AuctionLockManager();
        AtomicInteger counter = new AtomicInteger(0);

        manager.lockAndRun("key1", counter::incrementAndGet);

        assertEquals(1, counter.get());
    }

    @Test
    @DisplayName("Kiểm thử nhiều thread sử dụng cùng một lock key")
    void testMultipleThreadsSameKey() throws InterruptedException {
        AuctionLockManager manager = new AuctionLockManager();
        AtomicInteger counter = new AtomicInteger(0);
        String key = "shared-key";

        Thread t1 = new Thread(() -> {
            manager.lockAndRun(key, () -> {
                try {
                    Thread.sleep(500);
                    counter.incrementAndGet();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            });
        });

        Thread t2 = new Thread(() -> {
            manager.lockAndRun(key, counter::incrementAndGet);
        });

        t1.start();
        Thread.sleep(100); // Đảm bảo t1 lấy được lock trước
        t2.start();

        t1.join();
        t2.join();

        assertEquals(2, counter.get());
    }

    @Test
    @DisplayName("Kiểm thử tranh chấp lock dẫn đến timeout")
    void testLockTimeout() throws InterruptedException {
        AuctionLockManager manager = new AuctionLockManager();
        String key = "timeout-key";

        Thread t1 = new Thread(() -> {
            manager.lockAndRun(key, () -> {
                try {
                    Thread.sleep(3000); // Giữ lock lâu hơn 2 giây
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            });
        });

        t1.start();
        Thread.sleep(100);

        RuntimeException ex = assertThrows(RuntimeException.class, () -> {
            manager.lockAndRun(key, () -> {
            });
        });

        assertTrue(ex.getMessage().contains("Hệ thống đang bận"));
        t1.join();
    }
}
