package com.auction.server.core;

import com.auction.server.concurrency.AuctionLockManager;
import com.auction.server.repository.AuctionRepository;
import com.auction.server.service.AuthService;
import com.auction.shared.models.auction.Auction;
import com.auction.shared.models.auction.AuctionStatus;
import com.auction.shared.models.auction.BidTransaction;
import com.auction.shared.models.auth.Bidder;
import com.auction.shared.models.item.Item;
import com.auction.shared.models.auth.Seller;
import com.auction.shared.network.events.BalanceUpdatedEvent;
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
    private final AutoBidManager autoBidManager = new AutoBidManager();
    private final ThreadLocal<Boolean> isTriggering = ThreadLocal.withInitial(() -> false);

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

            // 2. ❌ ĐỌC SỐ DƯ NGOÀI LOCK - CÓ THỂ STALE
            BalanceUpdatedEvent refundEvent = new BalanceUpdatedEvent(
                    authService.getBalance(highestBidderUsername),  // Số dư đã cũ!
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
     * FIX RACE CONDITION:
     * - Xóa kiểm tra balance ngoài lock
     * - Lấy balance mới TRONG lock để tránh stale data
     * - Xóa hoàn tiền trùng lặp ngoài lock
     */
    public boolean placeBid(String auctionId, String bidderUsername, double amount) {
        try {
            Auction auction = auctionRepository.findById(auctionId);
            if (auction == null) {
                return false;
            }

            Bidder previousBidderObj = auction.getHighestBidder();
            String previousBidderUsername = (previousBidderObj != null) ? previousBidderObj.getUsername() : null;
            double previousPrice = auction.getCurrentPrice();

            String fullName = authService.getFullName(bidderUsername);
            Bidder bidder = new Bidder(bidderUsername, "", 0);
            bidder.setFullName(fullName);

            // ✅ CONTAINER để lưu các giá trị cần trả về từ trong lock
            double[] newBalanceRef = {0};
            double[] previousBidderNewBalanceRef = {0};
            boolean[] hasRefundPreviousBidder = {false};

            lockManager.lockAndRun(auction.getAuctionId(), () -> {
                // ✅ VALIDATE GIÁ ĐẤU NGAY TRONG LOCK
                auction.validateBid(amount);

                // ✅ KIỂM TRA VÀ TRỪ TIỀN NGAY TRONG LOCK - CHỈ 1 LẦN
                boolean froze = authService.freezeBalance(bidderUsername, amount);
                if (!froze) {
                    throw new IllegalArgumentException("Số dư không đủ. Vui lòng nạp thêm tiền!");
                }

                // ✅ LẤY SỐ DƯ MỚI NGAY TRONG LOCK
                newBalanceRef[0] = authService.getBalance(bidderUsername);

                // ✅ CẬP NHẬT AUCTION TRONG LOCK
                BidTransaction transaction = new BidTransaction("TX-" + System.currentTimeMillis(), bidder, amount, LocalDateTime.now());
                auction.updateAuctionState(bidder, amount, transaction);
                auctionRepository.save(auction);

                // ✅ HOÀN TIỀN NGƯỜI CŨ NGAY TRONG LOCK - CHỈ 1 LẦN
                if (previousBidderUsername != null && !previousBidderUsername.equals(bidderUsername)) {
                    authService.refundBalance(previousBidderUsername, previousPrice);
                    // Lấy số dư mới của người cũ ngay trong lock
                    previousBidderNewBalanceRef[0] = authService.getBalance(previousBidderUsername);
                    hasRefundPreviousBidder[0] = true;
                }
            });

            // ✅ GỬI EVENT BÊN NGOÀI LOCK SỬ DỤNG DỮ LIỆU LẤY TRONG LOCK
            BalanceUpdatedEvent deductEvent = new BalanceUpdatedEvent(
                    newBalanceRef[0],  // ✅ DỮ LIỆU CHÍNH XÁC LẤY TRONG LOCK
                    -amount,
                    null
            );
            com.auction.server.concurrency.ClientHandler.sendToUser(bidderUsername, deductEvent);

            // ✅ GỬI EVENT HOÀN TIỀN CHỈ 1 LẦN, KHÔNG TRÙNG LẶP
            if (hasRefundPreviousBidder[0]) {
                BalanceUpdatedEvent refundEvent = new BalanceUpdatedEvent(
                        previousBidderNewBalanceRef[0],  // ✅ DỮ LIỆU CHÍNH XÁC LẤY TRONG LOCK
                        previousPrice,
                        "Bạn đã bị vượt giá! " + previousPrice + "đ đã hoàn về ví."
                );
                com.auction.server.concurrency.ClientHandler.sendToUser(previousBidderUsername, refundEvent);
            }

            AuctionManager.getInstance().notifyObservers(auctionId,
                    auction.getBidHistory().get(auction.getBidHistory().size() - 1));

            // Kích hoạt đặt giá tự động (Auto-bid)
            triggerAutoBids(auctionId);

            return true;

        } catch (IllegalArgumentException e) {
            throw e;  // Ném exception để RequestRouter xử lý
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

    // --- BẮT ĐẦU: LOGIC AUTO-BID ---

    public void registerAutoBid(String auctionId, String bidderUsername, double maxBid) {
        autoBidManager.registerAutoBid(auctionId, bidderUsername, maxBid);
        triggerAutoBids(auctionId);
    }

    public void removeAutoBid(String auctionId, String bidderUsername) {
        autoBidManager.removeAutoBid(auctionId, bidderUsername);
    }

    public List<AutoBidRequest> getAutoBids(String auctionId) {
        return autoBidManager.getAutoBids(auctionId);
    }

    public void triggerAutoBids(String auctionId) {
        if (isTriggering.get()) {
            return;
        }
        isTriggering.set(true);
        try {
            while (true) {
                Auction auction = getAuctionById(auctionId);
                if (auction == null || auction.getStatus() != AuctionStatus.RUNNING) {
                    break;
                }

                AutoBidRequest eligible = autoBidManager.findEligibleAutoBidder(auctionId, auction);
                if (eligible == null) {
                    break;
                }

                double nextBidPrice = auction.getCurrentPrice() + auction.getStepPrice();

                double balance = authService.getBalance(eligible.getBidderUsername());
                if (balance < nextBidPrice) {
                    logger.warn("[AutoBid] User {} không đủ số dư để tự động đặt giá {} (Số dư: {}). Hủy Auto-bid.",
                            eligible.getBidderUsername(), nextBidPrice, balance);
                    autoBidManager.removeAutoBid(auctionId, eligible.getBidderUsername());
                    continue;
                }

                boolean success = false;
                try {
                    success = placeBid(auctionId, eligible.getBidderUsername(), nextBidPrice);
                } catch (Exception e) {
                    logger.warn("[AutoBid] Đặt giá tự động ném lỗi cho {} tại phòng {}: {}",
                            eligible.getBidderUsername(), auctionId, e.getMessage());
                }

                if (!success) {
                    autoBidManager.removeAutoBid(auctionId, eligible.getBidderUsername());
                }
            }
        } finally {
            isTriggering.set(false);
        }
    }
}