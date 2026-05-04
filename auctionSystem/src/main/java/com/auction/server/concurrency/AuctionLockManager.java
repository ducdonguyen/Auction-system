package com.auction.server.concurrency;


import com.auction.shared.exceptions.AuctionClosedException;
import com.auction.shared.exceptions.InvalidBidException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Quản lý các khóa (lock) cho từng phiên đấu giá để đảm bảo tính đồng bộ khi đặt thầu.
 */
public class AuctionLockManager {
  private final ConcurrentHashMap<String, ReentrantLock> locks = new ConcurrentHashMap<>();

  /**
   * Thực hiện một hành động trong khi giữ khóa cho một phiên đấu giá.
   *
   * @param lockKey ID phiên đấu giá (khóa).
   * @param action  Hành động cần thực hiện.
   */
  public void lockAndRun(String lockKey, Runnable action) {
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
        throw new InvalidBidException(
            "Hệ thống đang bận xử lý lượt đặt giá khác cho sản phẩm này!");
      }
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new AuctionClosedException("Tiến trình bị gián đoạn.");
    }
  }
}
