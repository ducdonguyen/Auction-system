package com.auction.server.concurrency;


import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;


public class AuctionLockManager {
    // Lưu trữ danh sách ổ khóa cho từng AuctionId.
    // ConcurrentHashMap đảm bảo việc quản lý các ổ khóa này cũng an toàn đa luồng.
    private final ConcurrentHashMap<String, ReentrantLock> locks = new ConcurrentHashMap<>();

    /**
     * Thực hiện logic đặt giá trong một khối khóa an toàn.
     *
     * @param lockKey ID của phiên đấu giá (auctionId hoặc itemId)
     * @param action  Đoạn code xử lý logic (kiểm tra giá, lưu DB)
     */
    public void executeWithLock(String lockKey, Runnable action) {
        // Lấy ổ khóa hiện có hoặc tạo mới nếu chưa có cho lockKey này
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
