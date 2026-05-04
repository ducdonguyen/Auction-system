package com.auction.client.network;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.auction.shared.models.AuctionStatus;
import com.auction.shared.models.BidTransaction;
import javafx.application.Platform;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicReference;

public class SocketClient {
    private static final Logger logger = LoggerFactory.getLogger(SocketClient.class);
    private static volatile SocketClient instance;
    private Socket socket;
    private ObjectOutputStream out;
    private ObjectInputStream in;

    // Cấu hình mạng
    private static final String SERVER_IP = "localhost";
    private static final int SERVER_PORT = 8080;

    // Hàng đợi để chứa các phản hồi (ServiceResult) thông thường
    private final BlockingQueue<Object> responseQueue = new LinkedBlockingQueue<>();

    // Điểm kết nối (Callback) để đẩy dữ liệu Realtime lên giao diện
    private final AtomicReference<RealtimeListener> realtimeListener = new AtomicReference<>();

    public interface RealtimeListener {
        void onNewBid(BidTransaction newBid);
        void onStatusUpdate(AuctionStatus newStatus);
    }

    // Chặn khởi tạo tự do để ép dùng Singleton
    private SocketClient() {
    }

    public static SocketClient getInstance() {
        if (instance == null) {
            synchronized (SocketClient.class) {
                if (instance == null) {
                    instance = new SocketClient();
                }
            }
        }
        return instance;
    }

    // Hàm để các Controller (như AuctionRoomController) đăng ký "nghe đài"
    public void setRealtimeListener(RealtimeListener listener) {
        this.realtimeListener.set(listener);
    }

    private void startListeningThread() {
        Thread listenerThread = new Thread(() -> {
            try {
                while (!Thread.currentThread().isInterrupted()) {
                    Object message = in.readObject();
                    handleIncomingMessage(message);
                }
            } catch (IOException | ClassNotFoundException e) {
                logger.info("[Client] Luồng lắng nghe đã dừng: {}", e.getMessage());
            } catch (InterruptedException e) {
                logger.error("[Client] Luồng lắng nghe bị ngắt quãng: ", e);
                Thread.currentThread().interrupt();
            }
        });
        listenerThread.setDaemon(true); // Luồng tự chết khi tắt App
        listenerThread.start();
    }

    private void handleIncomingMessage(Object message) throws InterruptedException {
        switch (message) {
            case BidTransaction bid -> handleBidTransaction(bid);
            case AuctionStatus status -> handleAuctionStatus(status);
            default -> responseQueue.put(message); // Nhét vào hàng đợi cho phản hồi bình thường
        }
    }

    private void handleBidTransaction(BidTransaction bid) {
        RealtimeListener listener = realtimeListener.get();
        if (listener != null) {
            Platform.runLater(() -> listener.onNewBid(bid));
        }
    }

    private void handleAuctionStatus(AuctionStatus status) {
        RealtimeListener listener = realtimeListener.get();
        if (listener != null) {
            Platform.runLater(() -> listener.onStatusUpdate(status));
        }
    }
    /**
     * Mở kết nối tới Server. Gọi hàm này lúc Client vừa bật lên.
     */
    public synchronized void connect() throws IOException {
        if (socket == null || socket.isClosed()) {
            socket = new Socket(SERVER_IP, SERVER_PORT);
            // LƯU Ý SỐNG CÒN: Phải tạo Output trước Input để tránh bị Deadlock (treo app)
            out = new ObjectOutputStream(socket.getOutputStream());
            in = new ObjectInputStream(socket.getInputStream());
            logger.info("[Client] Đã kết nối thành công tới Server {}:{}", SERVER_IP, SERVER_PORT);

            startListeningThread();
        }
    }

    /**
     * Gửi một gói tin (Object) lên Server
     */
    public synchronized void sendRequest(Object request) throws IOException {
        if (out != null) {
            out.writeObject(request);
            out.flush();
        }
    }

    /**
     * Lắng nghe phản hồi từ Server
     */
    public Object receiveResponse() throws InterruptedException {
        return responseQueue.take(); // Hàm này sẽ tự động block (chờ) cho đến khi có hàng trong Queue
    }

    /**
     * Ngắt kết nối an toàn khi tắt App
     */
    public synchronized void disconnect() {
        try {
            if (in != null) {
                in.close();
            }
            if (out != null) {
                out.close();
            }
            if (socket != null && !socket.isClosed()) {
                socket.close();
            }
            logger.info("[Client] Đã ngắt kết nối.");
        } catch (IOException e) {
            logger.error("[Client] Lỗi khi ngắt kết nối: ", e);
        }
    }
}
