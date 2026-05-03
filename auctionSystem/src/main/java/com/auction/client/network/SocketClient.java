package com.auction.client.network;

import com.auction.shared.models.AuctionStatus;
import com.auction.shared.models.BidTransaction;
import javafx.application.Platform;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

public class SocketClient {
    private static SocketClient instance;
    private Socket socket;
    private ObjectOutputStream out;
    private ObjectInputStream in;

    // Cấu hình mạng
    private static final String SERVER_IP = "localhost";
    private static final int SERVER_PORT = 8080;

    // Hàng đợi để chứa các phản hồi (ServiceResult) thông thường
    private final BlockingQueue<Object> responseQueue = new LinkedBlockingQueue<>();

    // Điểm kết nối (Callback) để đẩy dữ liệu Realtime lên giao diện
    private RealtimeListener realtimeListener;

    public interface RealtimeListener {
        void onNewBid(BidTransaction newBid);
        void onStatusUpdate(AuctionStatus newStatus);
    }

    // Chặn khởi tạo tự do để ép dùng Singleton
    private SocketClient() {
    }

    public static SocketClient getInstance() {
        if (instance == null) {
            instance = new SocketClient();
        }
        return instance;
    }

    // Hàm để các Controller (như AuctionRoomController) đăng ký "nghe đài"
    public void setRealtimeListener(RealtimeListener listener) {
        this.realtimeListener = listener;
    }

    private void startListeningThread() {
        Thread listenerThread = new Thread(() -> {
            try {
                while (true) {
                    Object message = in.readObject();

                    // Phân loại: Nếu là thông báo Realtime từ Đài phát thanh Server
                    if (message instanceof BidTransaction bid) {
                        if (realtimeListener != null) {
                            // Ép chạy trên luồng giao diện JavaFX để không bị lỗi văng app
                            Platform.runLater(() -> realtimeListener.onNewBid(bid));
                        }
                    } else if (message instanceof AuctionStatus status) {
                        if (realtimeListener != null) {
                            Platform.runLater(() -> realtimeListener.onStatusUpdate(status));
                        }
                    // Phân loại: Nếu là phản hồi bình thường (VD: ServiceResult của Login)
                    } else {
                        responseQueue.put(message); // Nhét vào hàng đợi
                    }
                }
            } catch (Exception e) {
                System.out.println("[Client] Luồng lắng nghe đã dừng.");
            }
        });
        listenerThread.setDaemon(true); // Luồng tự chết khi tắt App
        listenerThread.start();
    }

    /**
     * Mở kết nối tới Server. Gọi hàm này lúc Client vừa bật lên.
     */
    public void connect() throws IOException {
        if (socket == null || socket.isClosed()) {
            socket = new Socket(SERVER_IP, SERVER_PORT);
            // LƯU Ý SỐNG CÒN: Phải tạo Output trước Input để tránh bị Deadlock (treo app)
            out = new ObjectOutputStream(socket.getOutputStream());
            in = new ObjectInputStream(socket.getInputStream());
            System.out.println("[Client] Đã kết nối thành công tới Server " + SERVER_IP + ":" + SERVER_PORT);

            startListeningThread();
        }
    }

    /**
     * Gửi một gói tin (Object) lên Server
     */
    public void sendRequest(Object request) throws IOException {
        if (out != null) {
            out.writeObject(request);
            out.flush();
        }
    }

    /**
     * Lắng nghe phản hồi từ Server
     */
    public Object receiveResponse() throws Exception {
        return responseQueue.take(); // Hàm này sẽ tự động block (chờ) cho đến khi có hàng trong Queue
    }

    /**
     * Ngắt kết nối an toàn khi tắt App
     */
    public void disconnect() {
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
            System.out.println("[Client] Đã ngắt kết nối.");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
