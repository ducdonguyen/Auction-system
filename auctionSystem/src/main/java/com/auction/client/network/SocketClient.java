package com.auction.client.network;

import com.auction.shared.models.AuctionStatus;
import com.auction.shared.models.BidTransaction;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.Map;                                      // IMPORT MỚI
import java.util.concurrent.ConcurrentHashMap;             // IMPORT MỚI
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;                        // IMPORT MỚI
import javafx.application.Platform;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Lớp Client để kết nối và giao tiếp với Server qua Socket.
 */
public class SocketClient {
    private static final Logger logger = LoggerFactory.getLogger(SocketClient.class);
    private static volatile SocketClient instance;
    private Socket socket;
    private ObjectOutputStream out;
    private ObjectInputStream in;
    private final BlockingQueue<Object> responseQueue = new LinkedBlockingQueue<>();
    private final AtomicReference<RealtimeListener> realtimeListener = new AtomicReference<>();

    // THÊM MỚI: Bảng cấu trúc ánh xạ Class gói tin với bộ xử lý Callback tương ứng
    private final Map<Class<?>, Consumer<Object>> responseHandlers = new ConcurrentHashMap<>();

    /**
     * Interface để lắng nghe các sự kiện thời gian thực từ Server.
     */
    public interface RealtimeListener {
        void onNewBid(BidTransaction b);

        void onStatusUpdate(AuctionStatus s);
    }

    private SocketClient() {
    }

    /**
     * Lấy instance duy nhất của SocketClient.
     *
     * @return Instance của SocketClient.
     */
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

    /**
     * Ghi đè instance bằng Mock Object (Dành riêng cho Unit Test).
     *
     * @param mockInstance Đối tượng SocketClient giả.
     */
    public static void setInstance(SocketClient mockInstance) {
        instance = mockInstance;
    }

    public void setRealtimeListener(RealtimeListener l) {
        this.realtimeListener.set(l);
    }

    /**
     * PHƯƠNG THỨC MỚI THÊM VÀO: Đăng ký hàm xử lý riêng biệt cho từng loại Class phản hồi (Ví dụ: TopUpResponse.class)
     * Giúp Controller nhận được thông điệp thời gian thực từ Server mà không bị nghẽn mạch.
     */
    public void registerResponseHandler(Class<?> responseClass, Consumer<Object> handler) {
        if (responseClass != null && handler != null) {
            responseHandlers.put(responseClass, handler);
        }
    }

    private void startListeningThread() {
        Thread t = new Thread(() -> {
            try {
                while (!Thread.currentThread().isInterrupted()) {
                    handle(in.readObject());
                }
            } catch (Exception e) {
                logger.info("[Client] Listener stopped: {}", e.getMessage());
            }
        });
        t.setDaemon(true);
        t.start();
    }

    private void handle(Object m) throws InterruptedException {
        if (m == null) return;

        // 1. CẬP NHẬT: Kiểm tra xem gói tin nhận về có khớp với Class được đăng ký xử lý riêng không
        Consumer<Object> handler = responseHandlers.get(m.getClass());
        if (handler != null) {
            handler.accept(m);
            return; // Định tuyến xong, kết thúc sớm để không đẩy nhầm gói tin vào queue đồng bộ
        }

        // 2. GIỮ NGUYÊN: Xử lý các logic lọc dữ liệu cũ của hệ thống phòng đấu giá
        if (m instanceof BidTransaction b) {
            RealtimeListener l = realtimeListener.get();
            if (l != null) {
                Platform.runLater(() -> l.onNewBid(b));
            }
        } else if (m instanceof AuctionStatus s) {
            RealtimeListener l = realtimeListener.get();
            if (l != null) {
                Platform.runLater(() -> l.onStatusUpdate(s));
            }
        } else {
            // Đẩy vào hàng đợi đồng bộ cho các lệnh gọi đồng bộ dạng gửi/nhận như cũ (Ví dụ: CreateAuctionRequest)
            responseQueue.put(m);
        }
    }

    /**
     * Kết nối đến Server.
     *
     * @throws IOException Nếu xảy ra lỗi kết nối.
     */
    public synchronized void connect() throws IOException {
        if (socket == null || socket.isClosed()) {
            socket = new Socket("localhost", 8080);
            out = new ObjectOutputStream(socket.getOutputStream());
            in = new ObjectInputStream(socket.getInputStream());
            logger.info("[Client] Connected to Server.");
            startListeningThread();
        }
    }

    /**
     * Gửi yêu cầu đến Server.
     *
     * @param r Đối tượng yêu cầu.
     * @throws IOException Nếu xảy ra lỗi gửi.
     */
    public synchronized void sendRequest(Object r) throws IOException {
        if (out != null) {
            out.writeObject(r);
            out.flush();
        }
    }

    /**
     * Nhận phản hồi từ Server.
     *
     * @return Đối tượng phản hồi.
     * @throws InterruptedException Nếu luồng bị gián đoạn.
     */
    public Object receiveResponse() throws InterruptedException {
        return responseQueue.take();
    }

    /**
     * Ngắt kết nối với Server.
     */
    public synchronized void disconnect() {
        try {
            if (in != null) {
                if (in != null) {
                    in.close();
                }
            }
            if (out != null) {
                out.close();
            }
            if (socket != null) {
                socket.close();
            }
            logger.info("[Client] Disconnected.");
        } catch (IOException e) {
            logger.error("[Client] Disconnect error: {}", e.getMessage());
        }
    }
}