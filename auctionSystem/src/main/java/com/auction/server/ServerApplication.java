package com.auction.server;

import com.auction.server.concurrency.AuctionLockManager;
import com.auction.server.concurrency.ClientHandler;
import com.auction.server.core.AuctionScheduler;
import com.auction.server.core.AuctionService;
import com.auction.server.repository.AuctionRepository;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Lớp khởi chạy ứng dụng Server Đấu giá.
 */
public class ServerApplication {
  private static final Logger logger = LoggerFactory.getLogger(ServerApplication.class);
  // Định nghĩa cổng mạng để Client kết nối vào
  private static final int PORT = 8080;

  // Thêm cờ điều kiện dừng vòng lặp
  private static volatile boolean running = true;

  /**
   * Phương thức main để khởi chạy Server.
   *
   * @param args Tham số dòng lệnh.
   */
  public static void main(String[] args) {
    logger.info("=== SERVER ĐẤU GIÁ ĐANG KHỞI ĐỘNG ===");

    // Đăng ký Shutdown Hook để dừng Server an toàn khi tắt ứng dụng
    Runtime.getRuntime().addShutdownHook(new Thread(() -> {
      logger.info("Đang tắt Server...");
      running = false;
    }));

    try {
      logger.info("Đang kiểm tra cấu trúc cơ sở dữ liệu (MySQL)...");
      com.auction.server.config.DatabaseConfig.initializeDatabase();
      logger.info("Khởi tạo Database thành công!");

      // 1. KHỞI TẠO CÁC THÀNH PHẦN DÙNG CHUNG (Dựa trên SOLID)
      AuctionRepository auctionRepository = new AuctionRepository();
      AuctionLockManager lockManager = new AuctionLockManager();
      AuctionService auctionService = new AuctionService(lockManager, auctionRepository);

      // 2. KHỞI TẠO BỘ ĐẾM THỜI GIAN (TỰ ĐỘNG ĐÓNG/MỞ PHIÊN)
      // Tạo một cỗ máy thời gian dùng chung
      ScheduledExecutorService sharedScheduler = Executors.newScheduledThreadPool(1);
      AuctionScheduler auctionScheduler =
          new AuctionScheduler(auctionRepository, auctionService, sharedScheduler);

      // Bắt đầu quét thời gian để tự động chuyển trạng thái Auction
      auctionScheduler.startScheduling();

      // Bước 1: Mở cổng (Binding to Port)
      try (ServerSocket serverSocket = new ServerSocket(PORT)) {
        logger.info("Server đang lắng nghe kết nối tại cổng: {}", PORT);

        // Bước 2: Lắng nghe liên tục với điều kiện dừng
        while (running) {
          // Hàm accept() sẽ "đứng hình" (block) ở đây để chờ cho đến khi có 1 Client kết nối
          Socket clientSocket = serverSocket.accept();
          logger.info("--> CÓ KHÁCH! Client mới kết nối từ IP: {}", clientSocket.getInetAddress());

          // Bước 3: Chấp nhận và Cử nhân viên phục vụ (Accepting & Serving)
          // Tạo một ClientHandler (đã tích hợp AuctionObserver) để tiếp khách
          ClientHandler handler = new ClientHandler(clientSocket, auctionService);

          // Giao việc cho một Luồng (Thread) mới để Main Thread quay lại cửa đón khách tiếp theo
          Thread thread = new Thread(handler);
          thread.start();
        }
      }

    } catch (Exception e) {
      logger.error("Lỗi khi khởi động Server: ", e);
    }
  }
}