package com.auction.shared.models;

import com.auction.shared.exceptions.AuctionClosedException;
import com.auction.shared.exceptions.InvalidBidException;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Lớp Auction đại diện cho một phiên đấu giá trong hệ thống.
 * Triển khai Serializable để có thể gửi đối tượng qua Socket giữa Client và Server.
 */
public class Auction implements Serializable {
    // --- THUỘC TÍNH (ATTRIBUTES) ---

    private String auctionId;      // Mã định danh duy nhất cho mỗi phiên đấu giá
    private Item item;             // Món hàng đang được đem ra đấu giá (Electronics, Art, hoặc Vehicle)
    private Seller seller;         // Người tạo ra phiên đấu giá này
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    /**
     * currentPrice lưu trữ mức giá cao nhất hiện tại.
     * Ban đầu nó sẽ bằng giá khởi điểm (startingPrice).
     */
    private double currentPrice;

    /**
     * stepPrice là bước giá (khoảng cách giá tối thiểu giữa 2 lần đặt liên tiếp).
     * Ví dụ: Giá hiện tại 100, bước giá 10 -> Người sau phải đặt ít nhất 110.
     */
    private double stepPrice;

    /**
     * highestBidder lưu thông tin người đang giữ mức giá cao nhất.
     * Nếu chưa có ai đặt giá, giá trị này sẽ là null.
     */
    private Bidder highestBidder;

    /**
     * bidHistory danh sách lưu lại tất cả các giao dịch đặt giá đã thành công.
     * Giúp theo dõi diễn biến phiên đấu giá và phục vụ việc truy xuất lịch sử.
     */
    private List<BidTransaction> bidHistory;

    /**
     * status đại diện cho trạng thái vòng đời của phiên đấu giá.
     */
    private AuctionStatus status;

    /**
     * Constructor khởi tạo một phiên đấu giá mới.
     *
     * @param auctionId     Mã phiên
     * @param item          Đối tượng hàng hóa
     * @param seller        Người bán
     * @param startingPrice Giá khởi điểm ban đầu
     * @param stepPrice     Bước giá tối thiểu định trước
     */
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

    // --- LOGIC NGHIỆP VỤ (BUSINESS LOGIC) ---

    /**
     * Kiểm tra tính hợp lệ của một lệnh đặt giá mới.
     * @param amount Số tiền người dùng muốn đặt
     * @throws AuctionClosedException nếu phiên đấu giá không ở trạng thái RUNNING
     * @throws InvalidBidException nếu số tiền đặt không hợp lệ
     */
    public void validateBid(double amount) throws AuctionClosedException, InvalidBidException {
        if (this.status != AuctionStatus.RUNNING) {
            throw new AuctionClosedException("Phiên đấu giá " + auctionId + " hiện không trong trạng thái cho phép đặt giá.");
        }
        if (amount < (this.currentPrice + this.stepPrice)) {
            throw new InvalidBidException("Giá đặt " + amount + " không hợp lệ. Phải lớn hơn hoặc bằng " + (this.currentPrice + this.stepPrice));
        }
    }

    /**
     * Kiểm tra tính hợp lệ của một lệnh đặt giá mới (trả về boolean).
     * @param amount Số tiền người dùng muốn đặt
     * @return true nếu giá đặt hợp lệ, false nếu thấp hơn quy định.
     */
    public boolean isValidBid(double amount) {
        return amount >= (this.currentPrice + this.stepPrice);
    }

    /**
     * Cập nhật trạng thái mới cho phiên đấu giá khi có một lượt đặt giá thành công.
     * Lưu ý: Hàm này không chứa 'synchronized' vì việc đồng bộ luồng sẽ được quản lý
     * bởi lớp AuctionService để đảm bảo tính tập trung.
     * * @param bidder Người vừa đặt giá cao nhất thành công
     *
     * @param amount      Mức giá mới đã được phê duyệt
     * @param transaction Đối tượng giao dịch chứa chi tiết thời gian/thông tin để lưu lịch sử
     */
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

    // --- CÁC PHƯƠNG THỨC TRUY XUẤT (GETTERS & SETTERS) ---

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

    /**
     * Cập nhật trạng thái phiên đấu giá (Ví dụ: Chuyển từ RUNNING sang FINISHED)
     *
     * @param status Trạng thái mới
     */
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