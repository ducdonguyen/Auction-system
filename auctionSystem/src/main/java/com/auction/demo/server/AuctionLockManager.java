package com.auction.demo.server;

import org.springframework.stereotype.Component;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;
import java.util.concurrent.TimeUnit;

@Component
public class AuctionLockManager {
    // Lưu trữ danh sách ổ khóa cho từng ItemId.
    // ConcurrentHashMap đảm bảo việc quản lý các ổ khóa này cũng an toàn đa luồng.
    private final ConcurrentHashMap<Long, ReentrantLock> itemLocks = new ConcurrentHashMap<>();

    /**
     * Thực hiện logic đặt giá trong một khối khóa an toàn.
     * @param itemId ID của sản phẩm đang được đấu giá
     * @param action Đoạn code xử lý logic (kiểm tra giá, lưu DB)
     */
    public void executeWithLock(Long itemId, Runnable action) {
        // Lấy ổ khóa hiện có hoặc tạo mới nếu chưa có cho itemId này
        ReentrantLock lock = itemLocks.computeIfAbsent(itemId, k -> new ReentrantLock());

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
