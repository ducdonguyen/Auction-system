package com.auction.server.concurrency;


import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;


public class AuctionLockManager {
    private final ConcurrentHashMap<String, ReentrantLock> locks = new ConcurrentHashMap<>();

    public void executeWithLock(String lockKey, Runnable action) {
        ReentrantLock lock = locks.computeIfAbsent(lockKey, k -> new ReentrantLock());

        try {
            // Thử đợi để lấy khóa trong 2 giây (tránh tình trạng đứng máy chờ vô tận)
            if (lock.tryLock(2, TimeUnit.SECONDS)) {
                try {
                    action.run(); // Chạy logic nghiệp vụ
                } finally {
                    lock.unlock(); // Bắt buộc mở khóa trong finally
                }
            } else {
                throw new RuntimeException("Hệ thống đang bận xử lý lượt đặt giá khác cho sản phẩm này!");
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Tiến trình bị gián đoạn.");
        }
    }
}
