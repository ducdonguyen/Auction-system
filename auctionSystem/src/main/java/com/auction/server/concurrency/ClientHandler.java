package com.auction.server.concurrency;

import com.auction.server.core.AuctionManager;
import com.auction.server.core.AuctionObserver;
import com.auction.server.core.AuctionService;
import com.auction.shared.models.AuctionStatus;
import com.auction.shared.models.BidTransaction;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;

// Lớp này vừa là một Luồng (Runnable) vừa là một Người quan sát (AuctionObserver)
public class ClientHandler implements Runnable, AuctionObserver {
    private final Socket socket;
    private final AuctionService auctionService;
    private ObjectOutputStream out;
    private ObjectInputStream in;

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
        try {
            // Khởi tạo ống bơ truyền nhận dữ liệu (Phải tạo Output trước Input để tránh bị treo)
            out = new ObjectOutputStream(socket.getOutputStream());
            in = new ObjectInputStream(socket.getInputStream());

            // Vòng lặp giữ kết nối
            while (true) {
                Object request = in.readObject();
                // Xử lý request qua RequestRouter
                RequestRouter.route(request, this, out, auctionService);

                System.out.println("[ClientHandler] Nhận được yêu cầu từ Client: " +
                        request.getClass().getSimpleName());
            }

        } catch (IOException | ClassNotFoundException e) {
            System.out.println("Client đã ngắt kết nối hoặc dữ liệu truyền lên sai định dạng.");
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
            out.writeObject(newBid);
            out.flush(); // Đẩy đi ngay lập tức
            System.out.println("[ClientHandler] Đã gửi thông báo giá mới về cho Client.");
        } catch (Exception e) {
            System.err.println("[ClientHandler] Lỗi khi gửi thông báo giá mới: " + e.getMessage());
        }
    }

    @Override
    public void updateStatus(String auctionId, AuctionStatus newStatus) {
        try {
            // Gửi cập nhật trạng thái mới về cho Client qua Socket
            out.writeObject(newStatus);
            out.flush();
            System.out.println(
                    "[ClientHandler] Đã gửi thông báo cập nhật trạng thái (" + newStatus + ") về cho Client.");
        } catch (Exception e) {
            System.err.println("[ClientHandler] Lỗi khi gửi thông báo trạng thái: " + e.getMessage());
        }
    }
}
