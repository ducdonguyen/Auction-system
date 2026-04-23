package com.auction.shared.models;

import com.auction.shared.exceptions.AuctionClosedException;
import com.auction.shared.exceptions.InvalidBidException;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Auction implements Serializable {
    private static final long serialVersionUID = 1L;
    private String auctionId;      // Mã định danh duy nhất cho mỗi phiên đấu giá
    private Item item;             // Món hàng đang được đem ra đấu giá (Electronics, Art, hoặc Vehicle)
    private Seller seller;         // Người tạo ra phiên đấu giá này
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private double currentPrice;
    private double stepPrice;
    private Bidder highestBidder;
    private List<BidTransaction> bidHistory;
    private AuctionStatus status;

    public Auction(String auctionId, Item item, Seller seller, double startingPrice, double stepPrice,
                   LocalDateTime startTime, LocalDateTime endTime) {
        this.auctionId = auctionId;
        this.item = item;
        this.seller = seller;
        this.currentPrice = startingPrice; // Gán giá khởi điểm vào giá hiện tại để bắt đầu đấu giá
        this.stepPrice = stepPrice;
        this.bidHistory = new ArrayList<>(); // Khởi tạo danh sách rỗng để tránh NullPointerException
        this.status = AuctionStatus.OPEN;    // Mặc định phiên mới tạo sẽ ở trạng thái chờ mở
        this.startTime = startTime;
        this.endTime = endTime;
    }

    public boolean validateBid(double amount) throws AuctionClosedException, InvalidBidException {
        if (this.status != AuctionStatus.RUNNING) {
            throw new AuctionClosedException(
                    "Phiên đấu giá " + auctionId + " hiện không trong trạng thái cho phép đặt giá.");
        }
        if (amount < (this.currentPrice + this.stepPrice)) {
            throw new InvalidBidException("Giá đặt " + amount + " không hợp lệ. Phải lớn hơn hoặc bằng " +
                    (this.currentPrice + this.stepPrice));
        } else {
            return true;
        }
    }

    public void updateAuctionState(Bidder bidder, double amount, BidTransaction transaction) {
        // Cập nhật giá cao nhất hiện tại thành giá mới vừa đặt
        this.currentPrice = amount;

        // Ghi nhận người đang thắng thế hiện tại
        this.highestBidder = bidder;

        // Thêm giao dịch này vào lịch sử đấu giá của món hàng
        this.bidHistory.add(transaction);

        // In log nhẹ để kiểm tra trạng thái trên Console Server (tùy chọn)
        System.out.println("[LOG] Auction " + auctionId + " updated: " + amount + " by " + bidder.getUsername());
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

    /**
     * Trả về danh sách lịch sử đặt giá.
     * Sử dụng Collections.unmodifiableList để ngăn chặn việc các lớp bên ngoài
     * tự ý thêm/xóa phần tử trong danh sách này mà không thông qua logic của lớp Auction.
     * Đây là kỹ thuật bảo vệ dữ liệu (Data Integrity).
     */
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