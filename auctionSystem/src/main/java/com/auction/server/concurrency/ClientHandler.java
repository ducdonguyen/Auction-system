package com.auction.server.concurrency;

import com.auction.server.core.AuctionManager;
import com.auction.server.core.AuctionObserver;
import com.auction.server.service.AuthService;
import com.auction.shared.models.auction.AuctionStatus;
import com.auction.shared.models.auction.BidTransaction;
import com.auction.shared.network.events.AuctionTimeUpdatedEvent;
import java.util.concurrent.ConcurrentHashMap;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;

import com.auction.shared.network.responses.ServiceResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Lớp xử lý kết nối từ mỗi Client.
 * Lớp này vừa là một Luồng (Runnable) vừa là một Người quan sát (AuctionObserver).
 */
public class ClientHandler implements Runnable, AuctionObserver {
  private static final Logger logger = LoggerFactory.getLogger(ClientHandler.class);
  private static final ConcurrentHashMap<String, ClientHandler> activeClients = new ConcurrentHashMap<>();
  private String username;
  private final Socket socket;
  private final RequestRouter requestRouter;
  private final AuthService authService;
  private ObjectOutputStream out;

  // Biến lưu trữ ID của phiên đấu giá mà Client này đang xem
  private String currentWatchingAuctionId;

  /**
   * Khởi tạo ClientHandler với socket và dịch vụ đấu giá.
   *
   * @param socket         Socket kết nối với client.
   * @param requestRouter Bộ định tuyến tiếp nhận và xử lý yêu cầu từ client.
   * @param authService   Dịch vụ xác thực.
   */
  public ClientHandler(Socket socket, RequestRouter requestRouter, AuthService authService) {
    this.socket = socket;
    this.requestRouter = requestRouter;
    this.authService = authService;
  }

  public String getCurrentWatchingAuctionId() {
    return currentWatchingAuctionId;
  }

  public void setCurrentWatchingAuctionId(String auctionId) {
    this.currentWatchingAuctionId = auctionId;
  }

  // Hàm để gán username khi login và đưa vào danh bạ
  public void setUsername(String username) {
    this.username = username;
    activeClients.put(username, this);
  }

  // Hàm tĩnh để bắn gói tin Socket tới riêng một người dùng
  public static void sendToUser(String username, Object message) {
    ClientHandler handler = activeClients.get(username);
    if (handler != null && handler.out != null) {
      try {
        handler.out.writeObject(message);
        handler.out.flush();
        logger.info("[ClientHandler] Đã gửi thông báo cá nhân tới user: {}", username);
      } catch (IOException e) {
        logger.error("Lỗi khi gửi tin riêng cho {}: {}", username, e.getMessage());
      }
    }
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
        this.requestRouter.route(request, this, out);

        logger.info("[ClientHandler] Nhận được yêu cầu từ Client: {}",
            request.getClass().getSimpleName());
      }

    } catch (IOException | ClassNotFoundException e) {
      logger.info("Client đã ngắt kết nối hoặc dữ liệu truyền lên sai định dạng: {}",
          e.getMessage());
    } finally {
      // CỰC KỲ QUAN TRỌNG: Khi Client tắt app, phải xóa họ khỏi danh sách Observer
      // để tránh lỗi rò rỉ bộ nhớ (Memory Leak)
      if (currentWatchingAuctionId != null) {
        AuctionManager.getInstance().unsubscribe(currentWatchingAuctionId, this);
      }

      // Khi Client tắt app, nhớ xóa họ khỏi danh bạ
      if (this.username != null) {
        activeClients.remove(this.username);
      }
    }
  }

  // =========================================================
  // OBSERVER PATTERN TRÊN MẠNG
  // =========================================================
  @Override
  public void updateNewBid(String auctionId, BidTransaction newBid) {
    try {
      if (out != null) {
        if (newBid.bidder() != null) {
          String username = newBid.bidder().getUsername();
          String fullName = new com.auction.server.service.AuthService().getFullName(username);
          if (fullName != null && !fullName.trim().isEmpty()) {
            newBid.bidder().setFullName(fullName);
          }
        }
        out.writeObject(newBid);
        out.flush();
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
        logger.info("[ClientHandler] Đã gửi thông báo cập nhật trạng thái ({}) về cho Client.",
            newStatus);
      }
    } catch (IOException e) {
      logger.error("[ClientHandler] Lỗi khi gửi thông báo trạng thái: {}", e.getMessage());
    }
  }

  @Override
  public void updateTime(String auctionId, long newEndMillis) {
    try {
      if (out != null) {
        AuctionTimeUpdatedEvent ev = new AuctionTimeUpdatedEvent(auctionId, newEndMillis);
        out.writeObject(ev);
        out.flush();
        logger.info("[ClientHandler] Đã gửi thông báo cập nhật thời gian cho client: {} -> {}", auctionId, newEndMillis);
      }
    } catch (IOException e) {
      logger.error("[ClientHandler] Lỗi khi gửi updateTime: {}", e.getMessage());
    }
  }

  @Override
  public void receiveSystemMessage(String message) {
    try {
      // Đóng gói thành ServiceResult báo thành công và mang theo message + server time
      out.writeObject(new ServiceResult<>(true, message, null, System.currentTimeMillis()));
      out.flush();
    } catch (java.io.IOException e) {
      logger.error("Lỗi khi gửi tin nhắn hệ thống tới client: {}", e.getMessage(), e);
    }
  }
}
