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
import java.time.Duration;

/**
 * Dịch vụ xử lý các nghiệp vụ liên quan đến đấu giá
 */
public class AuctionService {
    private static final Logger logger = LoggerFactory.getLogger(AuctionService.class);
    private static final long ANTI_SNIPE_WINDOW_SECONDS = 30;     // cửa sổ phát hiện sniping (ví dụ 30s)
    private static final long ANTI_SNIPE_EXTENSION_SECONDS = 60;  // gia hạn thêm (ví dụ +60s)
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

            // Lấy thông tin người giữ giá cũ trước khi vào Lock
            Bidder previousBidderObj = auction.getHighestBidder();
            String previousBidderUsername = (previousBidderObj != null) ? previousBidderObj.getUsername() : null;
            double previousPrice = auction.getCurrentPrice();

            // Khởi tạo đối tượng Bidder mới trực tiếp bằng Username
            Bidder bidder = new Bidder(bidderUsername, "", 0);
            bidder.setFullName(bidderUsername); // Fallback dùng tạm Username làm tên hiển thị

            // CONTAINER hứng dữ liệu từ trong Lock mang ra ngoài gửi mạng
            double[] newBalanceRef = {0};
            double[] previousBidderNewBalanceRef = {0};
            boolean[] hasRefundPreviousBidder = {false};

            lockManager.lockAndRun(auction.getAuctionId(), () -> {
                // 1. KIỂM TRA LUẬT ĐẤU GIÁ
                auction.validateBid(amount);

                // Log balance before attempting to freeze
                double balanceBefore = authService.getBalance(bidderUsername);
                logger.info("[DEBUG] Balance before freeze for {}: {}", bidderUsername, balanceBefore);

                // 2. TRỪ TIỀN NGƯỜI MỚI (ĐÓNG BĂNG VÍ)
                boolean freezeResult = authService.freezeBalance(bidderUsername, amount);
                if (!freezeResult) {
                    logger.warn("[WARN] Freeze balance failed for {}: insufficient funds or DB error", bidderUsername);
                    throw new IllegalArgumentException("Số dư không đủ. Vui lòng nạp thêm tiền!");
                } else {
                    logger.info("[INFO] Freeze balance succeeded for {}: deducted {}", bidderUsername, amount);
                }
                // Retrieve balance after successful freeze
                double balanceAfterFreeze = authService.getBalance(bidderUsername);
                logger.info("[INFO] Balance after freeze for {}: {} (deducted {})", bidderUsername, balanceAfterFreeze, amount);
                newBalanceRef[0] = balanceAfterFreeze;

                // 3. CẬP NHẬT TRẠNG THÁI PHÒNG ĐẤU GIÁ
                BidTransaction transaction = new BidTransaction("TX-" + System.currentTimeMillis(), bidder, amount, LocalDateTime.now());
                auction.updateAuctionState(bidder, amount, transaction);

                // === ANTI-SNIPE MECHANISM ===
                LocalDateTime now = LocalDateTime.now();
                LocalDateTime end = auction.getEndTime();
                logger.info("[DEBUG] === ANTI-SNIPE CHECK === auctionId={}, now={}, endTime={}",
                        auction.getAuctionId(), now, end);
                if (end != null) {
                    long secondsLeft = Duration.between(now, end).getSeconds();
                    logger.info("[DEBUG] Anti-snipe: secondsLeft={}s (threshold={}s)",
                            secondsLeft, ANTI_SNIPE_WINDOW_SECONDS);
                    if (secondsLeft > 0 && secondsLeft <= ANTI_SNIPE_WINDOW_SECONDS) {
                        LocalDateTime newEnd = end.plusSeconds(ANTI_SNIPE_EXTENSION_SECONDS);
                        auction.setEndTime(newEnd);
                        logger.info("[ANTI-SNIPE] ✓ Auction {} EXTENDED by {}s | new end: {}",
                                auction.getAuctionId(), ANTI_SNIPE_EXTENSION_SECONDS, newEnd);
                    } else if (secondsLeft <= 0) {
                        logger.warn("[ANTI-SNIPE] ⚠ Cannot extend: Auction {} already ended ({}s ago)",
                                auction.getAuctionId(), Math.abs(secondsLeft));
                    }
                } else {
                    logger.warn("[ANTI-SNIPE] ⚠ Cannot extend: endTime is null for auction {}",
                            auction.getAuctionId());
                }
                auctionRepository.save(auction);

                // 4. HOÀN TIỀN CHO NGƯỜI CŨ (NẾU CÓ)
                if (previousBidderUsername != null && !previousBidderUsername.equals(bidderUsername)) {
                    authService.refundBalance(previousBidderUsername, previousPrice);
                    previousBidderNewBalanceRef[0] = authService.getBalance(previousBidderUsername);
                    hasRefundPreviousBidder[0] = true;
                }
            });

            // --- BÊN NGOÀI KHỐI LOCK: PHÁT SỰ KIỆN QUA MẠNG SOCKET ---

            // Bắn tin trừ tiền cho người mới
            com.auction.server.concurrency.ClientHandler.sendToUser(bidderUsername,
                    new BalanceUpdatedEvent(newBalanceRef[0], -amount, null));

            // Bắn tin hoàn tiền cho người cũ bị vượt giá
            if (hasRefundPreviousBidder[0]) {
                com.auction.server.concurrency.ClientHandler.sendToUser(previousBidderUsername,
                        new BalanceUpdatedEvent(previousBidderNewBalanceRef[0], previousPrice,
                                "Bạn đã bị vượt giá! " + previousPrice + "đ đã hoàn về ví."));
            }

            // Cập nhật bảng điện tử cho tất cả mọi người trong phòng
            AuctionManager.getInstance().notifyObservers(auctionId,
                    auction.getBidHistory().get(auction.getBidHistory().size() - 1));

            // Kích hoạt bot đặt giá tự động (nếu có cài đặt)
            triggerAutoBids(auctionId);

            return true;

        } catch (IllegalArgumentException e) {
            throw e; // Đẩy lên RequestRouter hiển thị thông báo Alert cho Client
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
            lockManager.lockAndRun(auction.getAuctionId(), () -> {
                // 1. Thực hiện các bước đặt giá và kiểm tra luật (Ví dụ: trừ tiền, update trạng thái...)
                performPlaceBid(auction, bidder, amount);

                // 2. === ANTI-SNIPE MECHANISM (Phải đặt TRONG khối Lock) ===
                LocalDateTime now = LocalDateTime.now();
                LocalDateTime end = auction.getEndTime();

                logger.info("[DEBUG] === ANTI-SNIPE CHECK === auctionId={}, now={}, endTime={}",
                        auction.getAuctionId(), now, end);

                if (end != null) {
                    long secondsLeft = Duration.between(now, end).getSeconds();
                    logger.info("[DEBUG] Anti-snipe: secondsLeft={}s (threshold={}s)",
                            secondsLeft, ANTI_SNIPE_WINDOW_SECONDS);

                    // CHỈ gia hạn khi phiên đấu giá ĐANG CHẠY và nằm trong khung giờ nhạy cảm
                    if (secondsLeft > 0 && secondsLeft <= ANTI_SNIPE_WINDOW_SECONDS) {
                        LocalDateTime newEnd = end.plusSeconds(ANTI_SNIPE_EXTENSION_SECONDS);
                        auction.setEndTime(newEnd);
                        logger.info("[ANTI-SNIPE] ✓ Auction {} EXTENDED by {}s | new end: {}",
                                auction.getAuctionId(), ANTI_SNIPE_EXTENSION_SECONDS, newEnd);
                    } else if (secondsLeft <= 0) {
                        logger.warn("[ANTI-SNIPE] ⚠ Cannot extend: Auction {} already ended ({}s ago)",
                                auction.getAuctionId(), Math.abs(secondsLeft));
                    }
                } else {
                    logger.warn("[ANTI-SNIPE] ⚠ Cannot extend: endTime is null for auction {}",
                            auction.getAuctionId());
                }

                // 3. Lưu vào Repository NGAY TRONG LOCK để tránh bị luồng khác đọc ghi đè dữ liệu cũ
                auctionRepository.save(auction);
            });

            return true;
        } catch (Exception e) {
            logger.warn("[FAILED] Bid rejected: {}", e.getMessage());
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