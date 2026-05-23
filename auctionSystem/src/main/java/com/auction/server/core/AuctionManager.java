package com.auction.server.core;

import com.auction.shared.exceptions.AuthenticationException;
import com.auction.shared.models.Auction;
import com.auction.shared.models.AuctionStatus;
import com.auction.shared.models.BidTransaction;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Quản lý các phiên đấu giá đang hoạt động và các người quan sát (Observer).
 */
public class AuctionManager {
  private static final Logger logger = LoggerFactory.getLogger(AuctionManager.class);
  private static volatile AuctionManager instance;
  private final Map<String, Auction> activeAuctions = new ConcurrentHashMap<>();
  private final Map<String, List<AuctionObserver>> observersMap = new ConcurrentHashMap<>();
  private final List<AuctionObserver> globalObservers = new CopyOnWriteArrayList<>();

  private AuctionManager() {
  }

  /**
   * Lấy instance duy nhất của AuctionManager (Singleton).
   *
   * @return Instance của AuctionManager.
   */
  public static AuctionManager getInstance() {
    if (instance == null) {
      synchronized (AuctionManager.class) {
        if (instance == null) {
          instance = new AuctionManager();
        }
      }
    }
    return instance;
  }

  /**
   * Thêm Client vào danh sách nhận thông báo toàn Server.
   */
  public void addGlobalObserver(AuctionObserver o) {
    if (!globalObservers.contains(o)) {
      globalObservers.add(o);
    }
  }

  /**
   * Xóa Client khỏi danh sách toàn Server khi họ ngắt kết nối.
   */
  public void removeGlobalObserver(AuctionObserver o) {
    globalObservers.remove(o);
  }

  /**
   * Bắn thông báo cho TẤT CẢ mọi người đang online.
   * @param message Nội dung thông báo
   */
  public void broadcastToAll(String message) {
    for (AuctionObserver o : globalObservers) {
      o.receiveSystemMessage(message);
    }
    logger.info("[BROADCAST] Đã gửi thông báo toàn Server: {}", message);
  }

  /**
   * Thêm một phiên đấu giá mới vào hệ thống.
   *
   * @param a Phiên đấu giá.
   * @param t Token xác thực quản trị viên.
   * @throws AuthenticationException Nếu token không hợp lệ.
   */
  public void addAuction(Auction a, String t) throws AuthenticationException {
    if (t == null || !t.equals("ADMIN_SECRET_TOKEN")) {
      throw new AuthenticationException("Invalid token.");
    }
    if (a == null || a.getAuctionId() == null) {
      throw new IllegalArgumentException("Invalid auction.");
    }
    if (activeAuctions.putIfAbsent(a.getAuctionId(), a) != null) {
      throw new IllegalArgumentException("Auction exists.");
    }
  }

  /**
   * Đăng ký theo dõi một phiên đấu giá.
   *
   * @param aid ID của phiên đấu giá.
   * @param o   Người quan sát.
   */
  public void subscribe(String aid, AuctionObserver o) {
    observersMap.computeIfAbsent(aid, k -> new CopyOnWriteArrayList<>()).add(o);
    logger.info("Subscribed to: {}", aid);
  }

  /**
   * Hủy đăng ký theo dõi một phiên đấu giá.
   *
   * @param aid ID của phiên đấu giá.
   * @param o   Người quan sát.
   */
  public void unsubscribe(String aid, AuctionObserver o) {
    List<AuctionObserver> obs = observersMap.get(aid);
    if (obs != null) {
      obs.remove(o);
      logger.info("Unsubscribed from: {}", aid);
    }
  }

  /**
   * Thông báo cho các người quan sát khi có giá thầu mới.
   *
   * @param aid ID của phiên đấu giá.
   * @param b   Giao dịch thầu mới.
   */
  public void notifyObservers(String aid, BidTransaction b) {
    List<AuctionObserver> obs = observersMap.get(aid);
    if (obs != null) {
      obs.forEach(o -> o.updateNewBid(aid, b));
    }
  }

  /**
   * Thông báo cho các người quan sát khi trạng thái phiên đấu giá thay đổi.
   *
   * @param aid ID của phiên đấu giá.
   * @param s   Trạng thái mới.
   */
  public void notifyStatusUpdate(String aid, AuctionStatus s) {
    List<AuctionObserver> obs = observersMap.get(aid);
    if (obs != null) {
      obs.forEach(o -> o.updateStatus(aid, s));
    }
  }
}
