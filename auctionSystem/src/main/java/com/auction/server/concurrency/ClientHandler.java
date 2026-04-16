package com.auction.server.concurrency;

import com.auction.server.core.AuctionManager;
import com.auction.server.core.AuctionObserver;
import com.auction.shared.models.BidTransaction;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;

// Lớp này vừa là một Luồng (Runnable) vừa là một Người quan sát (AuctionObserver)
public class ClientHandler implements Runnable, AuctionObserver {
    private final Socket socket;
    private ObjectOutputStream out;
    private ObjectInputStream in;

    // Biến lưu trữ ID của phiên đấu giá mà Client này đang xem
    private String currentWatchingAuctionId;

    public ClientHandler(Socket socket) {
        this.socket = socket;
    }

    @Override
    public void run() {
        try {
            // Khởi tạo ống bơ truyền nhận dữ liệu (Phải tạo Output trước Input để tránh bị treo)
            out = new ObjectOutputStream(socket.getOutputStream());
            in = new ObjectInputStream(socket.getInputStream());

            // TODO: Ở bước sau, ta sẽ viết code đọc lệnh (Command) từ Client gửi lên ở đây.
            // Ví dụ: Nhận lệnh "JOIN_ROOM_1", ta sẽ gọi:
            // currentWatchingAuctionId = "ROOM_1";
            // AuctionManager.getInstance().subscribe(1L, this);

            // Vòng lặp giữ kết nối
            while (true) {
                Object request = in.readObject();
                // Xử lý request...

                // S.O.L.I.D (SRP): Ủy quyền xử lý cho một class khác (Sẽ tạo sau).
                // ClientHandler không được phép chứa logic if-else dịch lệnh ở đây
                // Ví dụ: RequestRouter.route(request, this, out);

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
            System.err.println("[ClientHandler] Lỗi khi gửi thông báo qua mạng: " + e.getMessage());
        }
    }
}
