package com.auction.server.core;

import com.auction.shared.network.CreateAuctionRequest;
import com.auction.server.concurrency.AuctionLockManager;
import com.auction.server.repository.AuctionRepository;
import com.auction.server.service.AuthService;
import com.auction.shared.models.Auction;
import com.auction.shared.models.AuctionStatus;
import com.auction.shared.models.BidTransaction;
import com.auction.shared.models.Bidder;
import com.auction.shared.models.Item;
import com.auction.shared.models.Seller;
import com.auction.shared.network.BalanceUpdatedEvent;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Dịch vụ xử lý các nghiệp vụ liên quan đến đấu giá
 */
public class AuctionService {
    private static final Logger logger = LoggerFactory.getLogger(AuctionService.class);
    private AuctionLockManager lockManager;
    private AuctionRepository auctionRepository;
    private final AuthService authService = new AuthService();

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
     * Tạo một phiên đấu giá mới trực tiếp (Dùng cho Admin hoặc Hệ thống khởi tạo).
     */
    public Auction createAuction(Item item, Seller seller, double startingPrice, double stepPrice,
                                 LocalDateTime startTime, LocalDateTime endTime) {
        String id = "AUC-" + UUID.randomUUID().toString().substring(0, 8);
        Auction auction = new Auction(id, item, seller, startingPrice, stepPrice, startTime, endTime);
        auction.setStatus(AuctionStatus.PENDING);
        logger.info("[INFO] Created auction: {} for {}", id, item.getName());
        return auctionRepository.save(auction);
    }

    /**
     * Hủy một phiên đấu giá từ yêu cầu của Admin.
     *
     * @param auctionId ID của phiên đấu giá cần hủy.
     * @throws IllegalArgumentException nếu không tìm thấy ID.
     * @throws IllegalStateException nếu không thể chuyển trạng thái.
     */
    public void cancelAuction(String auctionId) {
        Auction auction = getAuctionById(auctionId);
        if (auction == null) {
            throw new IllegalArgumentException("Không tìm thấy phiên đấu giá với ID: " + auctionId);
        }

        // --- BẮT ĐẦU: LOGIC HOÀN TIỀN KHI ADMIN HỦY PHIÊN ---
        Bidder highestBidderObj = auction.getHighestBidder();
        if (highestBidderObj != null && highestBidderObj.getUsername() != null && !highestBidderObj.getUsername().isEmpty()) {
            String highestBidderUsername = highestBidderObj.getUsername();
            double amountToRefund = auction.getCurrentPrice();

            // 1. Cộng lại tiền vào ví
            authService.refundBalance(highestBidderUsername, amountToRefund);

            // 2. Bắn sự kiện bắt giao diện Client nhảy số dư
            BalanceUpdatedEvent refundEvent = new BalanceUpdatedEvent(
                    authService.getBalance(highestBidderUsername),
                    amountToRefund,
                    "Phiên đấu giá bị hủy. " + amountToRefund + "đ đã được hoàn về ví của bạn."
            );
            com.auction.server.concurrency.ClientHandler.sendToUser(highestBidderUsername, refundEvent);
        }
        // --- KẾT THÚC LOGIC HOÀN TIỀN ---

        boolean success = updateAuctionStatus(auction, AuctionStatus.CANCELED);
        if (!success) {
            throw new IllegalStateException("Trạng thái hiện tại không cho phép hủy.");
        }
        logger.info("[ADMIN ACTION] Phiên đấu giá {} đã bị hủy thành công.", auctionId);
    }

    /**
     * Lấy danh sách các phiên đấu giá đang chờ Admin duyệt.
     */
    public List<Auction> getPendingAuctions() {
        return auctionRepository.findPendingAuctions();
    }

    /**
     * Thực hiện đặt giá thầu cho một phiên đấu giá.
     */
    public boolean placeBid(String auctionId, String bidderUsername, double amount) {
        try {
            Auction auction = auctionRepository.findById(auctionId);
            if (auction == null) {
                return false;
            }

            // --- BẮT ĐẦU: KIỂM TRA SỐ DƯ ---
            double currentBalance = authService.getBalance(bidderUsername);
            if (currentBalance < amount) {
                // Đẩy lỗi ra để RequestRouter.handleBid gửi thông báo màu đỏ về màn hình người dùng
                throw new IllegalArgumentException("Số dư không đủ. Vui lòng nạp thêm tiền!");
            }
            // ---------------------------------

            Bidder previousBidderObj = auction.getHighestBidder();
            String previousBidderUsername = (previousBidderObj != null) ? previousBidderObj.getUsername() : null;
            double previousPrice = auction.getCurrentPrice();

            Bidder bidder = new Bidder(bidderUsername, "", 0);

            // Hàm này sẽ ném lỗi nếu bước giá không hợp lệ (validateBid)
            lockManager.lockAndRun(auction.getAuctionId(), () -> performPlaceBid(auction, bidder, amount));
            auctionRepository.save(auction);

            // --- BẮT ĐẦU: ĐÓNG BĂNG TIỀN VÀ HOÀN TIỀN CHO NGƯỜI CŨ ---
            // Trừ tiền người vừa đặt thành công
            authService.freezeBalance(bidderUsername, amount);

            // Nếu có người cũ bị vượt giá, hoàn tiền cho họ
            if (previousBidderUsername != null && !previousBidderUsername.isEmpty() && !previousBidderUsername.equals(bidderUsername)) {
                authService.refundBalance(previousBidderUsername, previousPrice);

                BalanceUpdatedEvent refundEvent = new BalanceUpdatedEvent(
                        authService.getBalance(previousBidderUsername),
                        previousPrice,
                        "Bạn đã bị vượt giá! " + previousPrice + "đ đã hoàn về ví."
                );
                com.auction.server.concurrency.ClientHandler.sendToUser(previousBidderUsername, refundEvent);
            }
            // ---------------------------------------------------------

            AuctionManager.getInstance().notifyObservers(auctionId,
                    auction.getBidHistory().get(auction.getBidHistory().size() - 1));
            return true;

        } catch (IllegalArgumentException e) {
            throw e; // Lỗi từ việc không đủ tiền hoặc sai bước giá ném thẳng ra ngoài
        } catch (Exception e) {
            logger.warn("[FAILED] Bid rejected: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Thực hiện đặt giá thầu.
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
     * Biểu đồ chuyển đổi trạng thái (Bao gồm cả PENDING).
     */
    private boolean isValidTransition(AuctionStatus current, AuctionStatus next) {
        if (current == next) {
            return true;
        }
        return switch (current) {
            case PENDING -> (next == AuctionStatus.OPEN || next == AuctionStatus.CANCELED);
            case OPEN -> (next == AuctionStatus.RUNNING || next == AuctionStatus.CANCELED);
            case RUNNING -> (next == AuctionStatus.FINISHED || next == AuctionStatus.CANCELED);
            case FINISHED -> (next == AuctionStatus.PAID || next == AuctionStatus.CANCELED);
            default -> false;
        };
    }
}