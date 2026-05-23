package com.auction.server.core;

import com.auction.shared.models.*;
import com.auction.shared.network.CreateAuctionRequest;
import com.auction.server.concurrency.AuctionLockManager;
import com.auction.server.repository.AuctionRepository;

import java.time.LocalDateTime;
import java.util.List;
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
     * Lấy thông tin phiên đấu giá theo ID.
     *
     * @param auctionId ID của phiên đấu giá.
     * @return Đối tượng Auction tương ứng, hoặc null nếu không tìm thấy.
     */
    public Auction getAuctionById(String auctionId) {
        return auctionRepository.findById(auctionId);
    }

    /**
     * Lấy toàn bộ danh sách phiên đấu giá.
     *
     * @return Danh sách các phiên đấu giá.
     */
    public List<Auction> getAllAuctions() {
        return auctionRepository.findAll();
    }

    /**
     * Tạo một phiên đấu giá mới trực tiếp (Hàm cũ - Thường dùng cho Admin hoặc Hệ thống khởi tạo sẵn).
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
     * CẬP NHẬT MỚI: Tiếp nhận và xử lý yêu cầu tạo phiên đấu giá từ Client dưới dạng PENDING (Chờ duyệt).
     *
     * @param request        Gói dữ liệu yêu cầu gửi từ Client.
     * @param sellerUsername Tên tài khoản của người gửi yêu cầu.
     * @return true nếu tiếp nhận và lưu thành công.
     */
    public boolean handleCreateAuctionRequest(CreateAuctionRequest request, String sellerUsername) {
        try {
            Item item;
            String type = request.getProductType();

            // Lấy các thông tin cơ bản từ request
            String name = request.getProductName();
            String desc = request.getDescription();
            double price = request.getStartingPrice();

            switch (type) {
                case "Điện tử": // Hoặc "Electronics" tùy ComboBox của bạn
                    // Truyền thêm giá trị mặc định cho warrantyMonths (Ví dụ: 12 tháng)
                    // Hoặc bạn có thể quy ước người dùng nhập vào mô tả rồi mình parse ra, nhưng để an toàn hãy để mặc định 12
                    item = new Electronics(name, desc, price, 12);
                    break;

                case "Xe cộ": // Hoặc "Vehicle"
                    // Truyền thêm giá trị mặc định cho brand (Ví dụ: "Chưa xác định")
                    item = new Vehicle(name, desc, price, "Chưa xác định");
                    break;

                case "Đồ cổ": // Hoặc "Art"
                    // Truyền thêm giá trị mặc định cho author (Ví dụ: "Ẩn danh")
                    item = new Art(name, desc, price, "Ẩn danh");
                    break;

                default:
                    throw new IllegalArgumentException("Loại sản phẩm không hợp lệ: " + type);
            }

            // Khởi tạo Seller khớp với constructor 2 tham số (username, password)
            Seller seller = new Seller(sellerUsername, "");

            // Thiết lập thời gian (Mặc định bắt đầu ngay, kéo dài trong 24 giờ)
            LocalDateTime startTime = LocalDateTime.now();
            LocalDateTime endTime = startTime.plusDays(1);

            // Sinh ID ngẫu nhiên cho phiên đấu giá mới
            String id = "AUC-" + UUID.randomUUID().toString().substring(0, 8);

            // Khởi tạo đối tượng Auction với trạng thái mặc định ban đầu là PENDING
            Auction auction = new Auction(id, item, seller, request.getStartingPrice(), request.getPriceStep(), startTime, endTime);
            auction.setStatus(AuctionStatus.PENDING);

            logger.info("[INFO] Nhận yêu cầu tạo phòng từ {}. Mã phiên: {} -> Trạng thái: PENDING", sellerUsername, id);

            // Lưu vào Cơ sở dữ liệu thông qua Repository
            auctionRepository.save(auction);
            return true;

        } catch (Exception e) {
            logger.error("[ERROR] Tạo phiên đấu giá thất bại: {}", e.getMessage(), e);
            return false;
        }
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

    /**
     * CẬP NHẬT MỚI: Thêm trạng thái PENDING vào biểu đồ chuyển đổi trạng thái (State Machine).
     */
    private boolean isValidTransition(AuctionStatus current, AuctionStatus next) {
        if (current == next) {
            return true;
        }
        return switch (current) {
            case PENDING -> (next == AuctionStatus.OPEN || next == AuctionStatus.CANCELED); // Cho phép Admin duyệt hoặc Huỷ
            case OPEN -> (next == AuctionStatus.RUNNING || next == AuctionStatus.CANCELED);
            case RUNNING -> (next == AuctionStatus.FINISHED || next == AuctionStatus.CANCELED);
            case FINISHED -> (next == AuctionStatus.PAID || next == AuctionStatus.CANCELED);
            default -> false;
        };
    }
}