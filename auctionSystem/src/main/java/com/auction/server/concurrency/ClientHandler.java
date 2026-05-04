package com.auction.server.concurrency;

import com.auction.server.core.AuctionManager;
import com.auction.server.core.AuctionObserver;
import com.auction.server.core.AuctionService;
import com.auction.shared.models.AuctionStatus;
import com.auction.shared.models.BidTransaction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;

// Lớp này vừa là một Luồng (Runnable) vừa là một Người quan sát (AuctionObserver)
public class ClientHandler implements Runnable, AuctionObserver {
    private static final Logger logger = LoggerFactory.getLogger(ClientHandler.class);
    private final Socket socket;
    private final AuctionService auctionService;
    private ObjectOutputStream out;

    // Biến lưu trữ ID của phiên đấu giá mà Client này đang xem
    private String currentWatchingAuctionId;

    public ClientHandler(Socket socket, AuctionService auctionService) {
        this.socket = socket;
        this.auctionService = auctionService;
    }

    public String getCurrentWatchingAuctionId() {
        return currentWatchingAuctionId;
    }

    public void setCurrentWatchingAuctionId(String auctionId) {
        this.currentWatchingAuctionId = auctionId;
    }

    @Override
    public void run() {
        // Khởi tạo ống bơ truyền nhận dữ liệu (Phải tạo Output trước Input để tránh bị treo)
        try (Socket s = this.socket;
             ObjectOutputStream o = new ObjectOutputStream(s.getOutputStream());
             ObjectInputStream i = new ObjectInputStream(s.getInputStream())) {

            this.out = o;

            // Vòng lặp giữ kết nối
            while (!Thread.currentThread().isInterrupted()) {
                Object request = i.readObject();
                // Xử lý request qua RequestRouter
                RequestRouter.route(request, this, out, auctionService);

                logger.info("[ClientHandler] Nhận được yêu cầu từ Client: {}",
                        request.getClass().getSimpleName());
            }

        } catch (IOException | ClassNotFoundException e) {
            logger.info("Client đã ngắt kết nối hoặc dữ liệu truyền lên sai định dạng: {}", e.getMessage());
        } finally {
            // CỰC KỲ QUAN TRỌNG: Khi Client tắt app, phải xóa họ khỏi danh sách Observer
            // để tránh lỗi rò rỉ bộ nhớ (Memory Leak)
            if (currentWatchingAuctionId != null) {
                AuctionManager.getInstance().unsubscribe(currentWatchingAuctionId, this);
            }
        }
    }

    // =========================================================
    // ĐÂY LÀ TRÁI TIM CỦA OBSERVER PATTERN TRÊN MẠNG
    // =========================================================
    @Override
    public void updateNewBid(String auctionId, BidTransaction newBid) {
        try {
            // Khi AuctionManager gọi hàm này, ClientHandler lập tức "nhồi" dữ liệu mới
            // vào đường ống mạng gửi thẳng về màn hình Client.
            if (out != null) {
                out.writeObject(newBid);
                out.flush(); // Đẩy đi ngay lập tức
                logger.info("[ClientHandler] Đã gửi thông báo giá mới về cho Client.");
            }
        } catch (IOException e) {
            logger.error("[ClientHandler] Lỗi khi gửi thông báo giá mới: {}", e.getMessage());
        }
    }

    @Override
    public void updateStatus(String auctionId, AuctionStatus newStatus) {
        try {
            // Gửi cập nhật trạng thái mới về cho Client qua Socket
            if (out != null) {
                out.writeObject(newStatus);
                out.flush();
                logger.info("[ClientHandler] Đã gửi thông báo cập nhật trạng thái ({}) về cho Client.", newStatus);
            }
        } catch (IOException e) {
            logger.error("[ClientHandler] Lỗi khi gửi thông báo trạng thái: {}", e.getMessage());
        }
    }
}
