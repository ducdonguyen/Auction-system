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

public class ServerApplication {
    // Định nghĩa cổng mạng để Client kết nối vào
    private static final int PORT = 8080;

    public static void main(String[] args) {
        System.out.println("=== SERVER ĐẤU GIÁ ĐANG KHỞI ĐỘNG ===");

        try {

            System.out.println("Đang kiểm tra cấu trúc cơ sở dữ liệu (MySQL)...");
            com.auction.server.config.DatabaseConfig.initializeDatabase();
            System.out.println("Khởi tạo Database thành công!");

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
                System.out.println("Server đang lắng nghe kết nối tại cổng: " + PORT);

                // Bước 2: Lắng nghe liên tục (Listening)
                while (true) {
                    // Hàm accept() sẽ "đứng hình" (block) ở đây để chờ cho đến khi có 1 Client kết nối
                    Socket clientSocket = serverSocket.accept();
                    System.out.println("--> CÓ KHÁCH! Client mới kết nối từ IP: " + clientSocket.getInetAddress());

                    // Bước 3: Chấp nhận và Cử nhân viên phục vụ (Accepting & Serving)
                    // Tạo một ClientHandler (đã tích hợp AuctionObserver) để tiếp khách
                    ClientHandler handler = new ClientHandler(clientSocket, auctionService);

                    // Giao việc cho một Luồng (Thread) mới để Main Thread quay lại cửa đón khách tiếp theo
                    Thread thread = new Thread(handler);
                    thread.start();
                }
            }

        } catch (Exception e) {
            System.err.println("Lỗi khi khởi động Server: " + e.getMessage());
        }
    }
}