package com.auction.server.core;

import com.auction.server.concurrency.AuctionLockManager;
import com.auction.server.repository.AuctionRepository;
import com.auction.shared.models.Auction;
import com.auction.shared.models.AuctionStatus;
import com.auction.shared.models.BidTransaction;
import com.auction.shared.models.Bidder;
import com.auction.shared.models.Item;
import com.auction.shared.models.Seller;
import java.time.LocalDateTime;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Dịch vụ xử lý các nghiệp vụ liên quan đến đấu giá.
 */
public class AuctionService {
  private static final Logger logger = LoggerFactory.getLogger(AuctionService.class);
  private AuctionLockManager lockManager;
  private AuctionRepository auctionRepository;

  public AuctionService() {
  }

  /**
   * Khởi tạo AuctionService với lock manager và repository.
   *
   * @param lm Lock manager.
   * @param ar Repository của phiên đấu giá.
   */
  public AuctionService(AuctionLockManager lm, AuctionRepository ar) {
    this.lockManager = lm;
    this.auctionRepository = ar;
  }

  /**
   * Tạo một phiên đấu giá mới.
   *
   * @param item          Sản phẩm.
   * @param seller        Người bán.
   * @param startingPrice Giá khởi điểm.
   * @param stepPrice     Bước giá.
   * @param startTime     Thời gian bắt đầu.
   * @param endTime       Thời gian kết thúc.
   * @return Phiên đấu giá vừa tạo.
   */
  public Auction createAuction(Item item, Seller seller, double startingPrice, double stepPrice,
                               LocalDateTime startTime, LocalDateTime endTime) {
    String id = "AUC-" + UUID.randomUUID().toString().substring(0, 8);
    Auction auction = new Auction(id, item, seller, startingPrice, stepPrice, startTime, endTime);
    auction.setStatus(AuctionStatus.OPEN);
    logger.info("[INFO] Created auction: {} for {}", id, item.getName());
    return auctionRepository.save(auction);
  }

  /**
   * Thực hiện đặt giá thầu cho một phiên đấu giá.
   *
   * @param auctionId      ID của phiên đấu giá.
   * @param bidderUsername Tên người đặt thầu.
   * @param amount         Số tiền thầu.
   * @return true nếu đặt thầu thành công.
   */
  public boolean placeBid(String auctionId, String bidderUsername, double amount) {
    try {
      Auction auction = auctionRepository.findById(auctionId);
      if (auction == null) {
        return false;
      }
      Bidder bidder = new Bidder(bidderUsername, "", 0);
      lockManager.lockAndRun(auction.getAuctionId(), () -> performPlaceBid(auction, bidder, amount));
      auctionRepository.save(auction);
      AuctionManager.getInstance().notifyObservers(auctionId,
          auction.getBidHistory().get(auction.getBidHistory().size() - 1));
      return true;
    } catch (Exception e) {
      logger.warn("[FAILED] Bid rejected: {}", e.getMessage());
      return false;
    }
  }

  /**
   * Thực hiện đặt giá thầu.
   *
   * @param auction Phiên đấu giá.
   * @param bidder  Người đặt thầu.
   * @param amount  Số tiền.
   * @return true nếu thành công.
   */
  public boolean placeBid(Auction auction, Bidder bidder, double amount) {
    try {
      lockManager.lockAndRun(auction.getAuctionId(), () -> performPlaceBid(auction, bidder, amount));
      auctionRepository.save(auction);
      return true;
    } catch (Exception e) {
      logger.warn("[FAILED] {}", e.getMessage());
      return false;
    }
  }

  private void performPlaceBid(Auction auction, Bidder bidder, double amount) {
    auction.validateBid(amount);
    BidTransaction transaction = new BidTransaction("TX-" + System.currentTimeMillis(),
        bidder, amount, LocalDateTime.now());
    auction.updateAuctionState(bidder, amount, transaction);
    logger.info("[SUCCESS] {} bid: {}", bidder.getUsername(), amount);
  }

  /**
   * Cập nhật trạng thái cho phiên đấu giá.
   *
   * @param auction    Phiên đấu giá.
   * @param nextStatus Trạng thái tiếp theo.
   * @return true nếu chuyển trạng thái hợp lệ.
   */
  public boolean updateAuctionStatus(Auction auction, AuctionStatus nextStatus) {
    if (isValidTransition(auction.getStatus(), nextStatus)) {
      logger.info("[INFO] Auction {}: {} -> {}", auction.getAuctionId(),
          auction.getStatus(), nextStatus);
      auction.setStatus(nextStatus);
      auctionRepository.save(auction);
      AuctionManager.getInstance().notifyStatusUpdate(auction.getAuctionId(), nextStatus);
      return true;
    }
    return false;
  }

  private boolean isValidTransition(AuctionStatus current, AuctionStatus next) {
    if (current == next) {
      return true;
    }
    return switch (current) {
      case OPEN -> (next == AuctionStatus.RUNNING || next == AuctionStatus.CANCELED);
      case RUNNING -> (next == AuctionStatus.FINISHED || next == AuctionStatus.CANCELED);
      case FINISHED -> (next == AuctionStatus.PAID || next == AuctionStatus.CANCELED);
      default -> false;
    };
  }
}