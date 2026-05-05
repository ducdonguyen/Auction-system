package com.auction.shared.models;

import com.auction.shared.exceptions.AuctionClosedException;
import com.auction.shared.exceptions.InvalidBidException;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Lớp đại diện cho một phiên đấu giá.
 */
public class Auction implements Serializable {
  private static final Logger logger = LoggerFactory.getLogger(Auction.class);
  private static final long serialVersionUID = 1L;
  private String auctionId;
  private Item item;
  private Seller seller;
  private LocalDateTime startTime;
  private LocalDateTime endTime;
  private double currentPrice;
  private final double stepPrice;
  private Bidder highestBidder;
  private final List<BidTransaction> bidHistory = new ArrayList<>();
  private AuctionStatus status = AuctionStatus.OPEN;

  /**
   * Khởi tạo một phiên đấu giá mới.
   *
   * @param auctionId     ID của phiên đấu giá.
   * @param item          Sản phẩm được đấu giá.
   * @param seller        Người bán.
   * @param startingPrice Giá khởi điểm.
   * @param stepPrice     Bước giá tối thiểu.
   * @param startTime     Thời gian bắt đầu.
   * @param endTime       Thời gian kết thúc.
   */
  public Auction(String auctionId, Item item, Seller seller, double startingPrice,
                 double stepPrice, LocalDateTime startTime, LocalDateTime endTime) {
    this.auctionId = auctionId;
    this.item = item;
    this.seller = seller;
    this.currentPrice = startingPrice;
    this.stepPrice = stepPrice;
    this.startTime = startTime;
    this.endTime = endTime;
  }

  /**
   * Kiểm tra xem giá thầu có hợp lệ hay không.
   *
   * @param amount Số tiền thầu.
   * @return true nếu giá thầu hợp lệ.
   * @throws AuctionClosedException Nếu phiên đấu giá không còn chạy.
   * @throws InvalidBidException    Nếu giá thầu không đáp ứng yêu cầu.
   */
  public boolean validateBid(double amount) throws AuctionClosedException, InvalidBidException {
    if (status != AuctionStatus.RUNNING) {
      throw new AuctionClosedException("Auction " + auctionId + " not running.");
    }
    if (amount <= 0) {
      throw new InvalidBidException("Bid must be > 0.");
    }
    if (amount < (currentPrice + stepPrice)) {
      throw new InvalidBidException("Bid " + amount + " too low. Min: " + (currentPrice + stepPrice));
    }
    return true;
  }

  /**
   * Cập nhật trạng thái của phiên đấu giá khi có giá thầu mới.
   *
   * @param bidder      Người đặt thầu mới.
   * @param amount      Số tiền thầu mới.
   * @param transaction Giao dịch thầu tương ứng.
   */
  public void updateAuctionState(Bidder bidder, double amount, BidTransaction transaction) {
    this.currentPrice = amount;
    this.highestBidder = bidder;
    if (transaction != null) {
      this.bidHistory.add(transaction);
    }
    logger.debug("[LOG] Auction {} updated: {} by {}", auctionId, amount, bidder.getUsername());
  }

  public double getCurrentPrice() {
    return currentPrice;
  }

  public double getStepPrice() {
    return stepPrice;
  }

  public Bidder getHighestBidder() {
    return highestBidder;
  }

  public List<BidTransaction> getBidHistory() {
    return Collections.unmodifiableList(bidHistory);
  }

  public String getAuctionId() {
    return auctionId;
  }

  public void setAuctionId(String auctionId) {
    this.auctionId = auctionId;
  }

  public Item getItem() {
    return item;
  }

  public void setItem(Item item) {
    this.item = item;
  }

  public Seller getSeller() {
    return seller;
  }

  public void setSeller(Seller seller) {
    this.seller = seller;
  }

  public AuctionStatus getStatus() {
    return status;
  }

  public void setStatus(AuctionStatus status) {
    this.status = status;
  }

  public void setStartTime(LocalDateTime startTime) {
    this.startTime = startTime;
  }

  public LocalDateTime getStartTime() {
    return startTime;
  }

  public void setEndTime(LocalDateTime endTime) {
    this.endTime = endTime;
  }

  public LocalDateTime getEndTime() {
    return endTime;
  }
}