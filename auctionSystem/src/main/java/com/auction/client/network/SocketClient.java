package com.auction.client.network;

import com.auction.shared.models.auction.AuctionStatus;
import com.auction.shared.models.auction.BidTransaction;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Consumer;

import com.auction.shared.network.events.BalanceUpdatedEvent;
import javafx.application.Platform;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Lớp Client để kết nối và giao tiếp với Server qua Socket.
 */
public class SocketClient {
  private static final Logger logger = LoggerFactory.getLogger(SocketClient.class); // Bộ ghi log để theo dõi trạng thái mạng hệ thống.
  private static volatile SocketClient instance; // Biến lưu trữ instance duy nhất (mô hình Singleton), từ khóa volatile đảm bảo đồng bộ luồng.

  private Socket socket; // Đối tượng kết nối mạng TCP thuần túy.
  private ObjectOutputStream out; // Luồng dữ liệu đầu ra để gửi Object Java lên Server.
  private ObjectInputStream in;   // Luồng dữ liệu đầu vào để đọc Object Java từ Server gửi về.

  // Hàng đợi chặn luồng: Lưu trữ tạm thời các gói tin phản hồi mang tính đồng bộ (Request-Response).
  private final BlockingQueue<Object> responseQueue = new LinkedBlockingQueue<>();

  // Tham chiếu nguyên tử lưu trữ bộ lắng nghe real-time (dành cho phòng đấu giá) để tránh xung đột đa luồng.
  private final AtomicReference<RealtimeListener> realtimeListener = new AtomicReference<>();

  // Bảng băm an toàn đa luồng: Dùng để đăng ký các hàm xử lý phản hồi (Handler) dựa trên lớp dữ liệu (Class).
  private final ConcurrentHashMap<Class<?>, Consumer<Object>> handlers = new ConcurrentHashMap<>();

  /**
   * Interface để lắng nghe các sự kiện thời gian thực từ Server.
   */
  public interface RealtimeListener {
    void onNewBid(BidTransaction b);

    void onStatusUpdate(AuctionStatus s);

    void onBalanceUpdate(double newBalance, double amountChanged, String reason);
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

  public <T> void registerResponseHandler(Class<T> clazz, Consumer<Object> handler) {
    handlers.put(clazz, handler);
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
    } else if (m instanceof BalanceUpdatedEvent b) {
      RealtimeListener l = realtimeListener.get();
      if (l != null) {
        Platform.runLater(() -> l.onBalanceUpdate(b.getNewBalance(), b.getAmountChanged(), b.getReason()));
      }
    } else {
      Consumer<Object> handler = handlers.get(m.getClass());
      if (handler != null) {
        handler.accept(m);
      } else {
        responseQueue.put(m);
      }
    }
  }

  /**
   * Kết nối đến Server.
   *
   * @throws IOException Nếu xảy ra lỗi kết nối.
   */
  public synchronized void connect() throws IOException {
    if (socket == null || socket.isClosed()) {
      socket = new Socket("0.tcp.ap.ngrok.io", 15759);
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
   * Nhận phản hồi từ Server với thời gian chờ tùy chỉnh.
   *
   * @param timeoutSeconds Thời gian chờ tối đa (tính bằng giây).
   * @return Đối tượng phản hồi.
   * @throws InterruptedException Nếu luồng bị gián đoạn trong lúc chờ.
   * @throws TimeoutException Nếu server không phản hồi sau khoảng thời gian quy định.
   */
  public Object receiveResponse(long timeoutSeconds) throws InterruptedException, TimeoutException {
    // Sử dụng poll thay vì take để có thể set timeout
    Object response = responseQueue.poll(timeoutSeconds, TimeUnit.SECONDS);

    // Nếu hết thời gian mà vẫn chưa có dữ liệu (response == null)
    if (response == null) {
      throw new TimeoutException("Server không phản hồi trong vòng " + timeoutSeconds + " giây.");
    }
    return response;
  }

  /**
   * Nhận phản hồi từ Server với thời gian chờ mặc định.
   *
   * @return Đối tượng phản hồi.
   * @throws InterruptedException Nếu luồng bị gián đoạn.
   * @throws TimeoutException Nếu server không phản hồi sau 10 giây.
   */
  public Object receiveResponse() throws InterruptedException, TimeoutException {
    return receiveResponse(10); // Mặc định chờ 10 giây
  }

  /**
   * Ngắt kết nối với Server.
   */
  public synchronized void disconnect() {
    try {
      if (in != null) {
        in.close();
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
