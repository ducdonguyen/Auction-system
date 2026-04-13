package com.auction.server.core;

import com.auction.server.concurrency.AuctionLockManager;
import com.auction.server.repository.AuctionRepository;
import com.auction.shared.models.*;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Lớp dịch vụ điều phối các hoạt động của phiên đấu giá.
 * Đây là nơi tập trung logic nghiệp vụ và xử lý tranh chấp đa luồng.
 * Sử dụng AuctionLockManager để quản lý locking an toàn cho từng phiên.
 */
@Service
public class AuctionService {

    @Autowired
    private AuctionLockManager lockManager;

    @Autowired
    private AuctionRepository auctionRepository;

    public AuctionService() {
    }

    public AuctionService(AuctionLockManager lockManager) {
        this.lockManager = lockManager;
    }

    /**
     * CHỨC NĂNG 1: TẠO PHIÊN ĐẤU GIÁ MỚI
     * Công dụng: Khởi tạo một phiên đấu giá và đưa nó vào hệ thống.
     */
    @Transactional
    public Auction createAuction(Item item, Seller seller, double startingPrice, double stepPrice, LocalDateTime startTime, LocalDateTime endTime) {
        // Tạo một ID duy nhất bằng UUID (tránh trùng lặp ID phiên)
        String auctionId = "AUC -" + UUID.randomUUID().toString().substring(0, 8);

        // Khởi tạo đối tượng Auction
        Auction newAuction = new Auction(auctionId, item, seller, startingPrice, stepPrice, startTime, endTime);

        // Mặc định phiên mới tạo sẽ ở trạng thái OPEN
        newAuction.setStatus(AuctionStatus.OPEN);

        System.out.println("[INFO] Đã tạo phiên đấu giá mới: " + auctionId + " cho mặt hàng " + item.getName());
        return auctionRepository.save(newAuction);
    }

    /**
     * CHỨC NĂNG 2: ĐẶT GIÁ (PLACE BID)
     * Công dụng: Xử lý tranh chấp và cập nhật giá hiện tại.
     * Sử dụng AuctionLockManager để lock từng phiên đấu giá, đảm bảo thread-safe.
     */
    @Transactional
    public boolean placeBid(Auction auction, Bidder bidder, double bidAmount) {
        final boolean[] success = {false};

        // Sử dụng lock manager để đảm bảo chỉ một người có thể đặt giá cho phiên này tại một thời điểm
        lockManager.executeWithLock(auction.getAuctionId(), () -> {
            success[0] = performPlaceBid(auction, bidder, bidAmount);
        });

        if (success[0]) {
            auctionRepository.save(auction);
        }

        return success[0];
    }

    /**
     * Xử lý logic đặt giá (chạy bên trong lock)
     */
    private boolean performPlaceBid(Auction auction, Bidder bidder, double bidAmount) {
        // 1. Kiểm tra trạng thái phiên đấu giá (Phải đang RUNNING mới được đặt)
        if (auction.getStatus() != AuctionStatus.RUNNING) {
            System.out.println("[FAILED] Đấu giá không ở trạng thái RUNNING. Trạng thái hiện tại: " + auction.getStatus());
            return false;
        }

        // 2. Kiểm tra tính hợp lệ của số tiền đặt (Gọi logic từ Model Auction)
        // Giá mới phải >= Giá hiện tại + Bước giá
        if (!auction.isValidBid(bidAmount)) {
            System.out.println("[FAILED] Giá đặt " + bidAmount + " không hợp lệ so với giá hiện tại "
                    + auction.getCurrentPrice() + " (Bước giá: " + auction.getStepPrice() + ")");
            return false;
        }

        // 3. Nếu vượt qua các bước trên, tiến hành cập nhật trạng thái
        // Tạo một đối tượng giao dịch mới để lưu vào lịch sử
        BidTransaction transaction = new BidTransaction(
                "TX -" + System.currentTimeMillis(), // Tạo mã giao dịch tạm thời dựa trên thời gian
                bidder,
                bidAmount,
                LocalDateTime.now()
        );

        // Gọi hàm cập nhật dữ liệu nội bộ của đối tượng Auction
        auction.updateAuctionState(bidder, bidAmount, transaction);

        System.out.println("[SUCCESS] " + bidder.getUsername() + " đã đặt giá thành công: " + bidAmount);
        return true;
    }
    /**
     * CHỨC NĂNG 3: CHUYỂN ĐỔI TRẠNG THÁI (STATUS TRANSITION)
     * Công dụng: Đảm bảo phiên đấu giá tuân thủ đúng vòng đời (OPEN -> RUNNING -> FINISHED).
     */
    @Transactional
    public boolean updateAuctionStatus(Auction auction, AuctionStatus nextStatus) {
        // Kiểm tra xem việc chuyển từ trạng thái hiện tại sang trạng thái mới có hợp lệ không
        if (isValidTransition(auction.getStatus(), nextStatus)) {
            AuctionStatus oldStatus = auction.getStatus();
            auction.setStatus(nextStatus);
            auctionRepository.save(auction);

            System.out.println("[INFO] Auction " + auction.getAuctionId()
                    + ": " + oldStatus + " -> " + nextStatus);

            // TODO: Nơi đây bạn có thể thêm logic Broadcast qua Socket sau này
            // broadcastStatusUpdate(auction);

            return true;
        } else {
            System.err.println("[FAILED] Không thể chuyển trạng thái từ "
                    + auction.getStatus() + " sang " + nextStatus);
            return false;
        }
    }

    /**
     * Kiểm tra tính hợp lệ của máy trạng thái (State Machine)
     */
    private boolean isValidTransition(AuctionStatus current, AuctionStatus next) {
        if (current == next) return true; // Không thay đổi gì

        return switch (current) {
            case OPEN -> (next == AuctionStatus.RUNNING || next == AuctionStatus.CANCELED);
            case RUNNING -> (next == AuctionStatus.FINISHED || next == AuctionStatus.CANCELED);
            case FINISHED -> (next == AuctionStatus.PAID || next == AuctionStatus.CANCELED);
            default -> false; // PAID hoặc CANCELED là trạng thái cuối, không thể chuyển đi tiếp
        };
    }
}