package com.auction.server.core;

import com.auction.server.concurrency.AuctionLockManager;
import com.auction.server.repository.AuctionRepository;
import com.auction.shared.exceptions.AuctionClosedException;
import com.auction.shared.exceptions.InvalidBidException;
import com.auction.shared.models.Auction;
import com.auction.shared.models.AuctionStatus;
import com.auction.shared.models.BidTransaction;
import com.auction.shared.models.Bidder;
import com.auction.shared.models.Item;
import com.auction.shared.models.Seller;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Lớp dịch vụ điều phối các hoạt động của phiên đấu giá.
 * Đây là nơi tập trung logic nghiệp vụ và xử lý tranh chấp đa luồng.
 * Sử dụng AuctionLockManager để quản lý locking an toàn cho từng phiên.
 */

public class AuctionService {

    private AuctionLockManager lockManager;
    private AuctionRepository auctionRepository;

    public AuctionService() {
    }


    public AuctionService(AuctionLockManager lockManager, AuctionRepository auctionRepository) {
        this.lockManager = lockManager;
        this.auctionRepository = auctionRepository;
    }

    /**
     * CHỨC NĂNG 1: TẠO PHIÊN ĐẤU GIÁ MỚI
     * Công dụng: Khởi tạo một phiên đấu giá và đưa nó vào hệ thống.
     */

    public Auction createAuction(Item item, Seller seller, double startingPrice, double stepPrice,
                                 LocalDateTime startTime, LocalDateTime endTime) {
        // Tạo một ID duy nhất bằng UUID (tránh trùng lặp ID phiên)
        String auctionId = "AUC-" + UUID.randomUUID().toString().substring(0, 8);

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

    public boolean placeBid(Auction auction, Bidder bidder, double bidAmount) {
        try {
            // Sử dụng lock manager để đảm bảo chỉ một người có thể đặt giá cho phiên này tại một thời điểm
            lockManager.executeWithLock(auction.getAuctionId(), () -> {
                performPlaceBid(auction, bidder, bidAmount);
            });

            auctionRepository.save(auction);
            return true;
        } catch (AuctionClosedException | InvalidBidException e) {
            System.err.println("[FAILED] " + e.getMessage());
            return false;
        } catch (Exception e) {
            System.err.println("[ERROR] Lỗi không xác định khi đặt giá: " + e.getMessage());
            return false;
        }
    }

    /**
     * Xử lý logic đặt giá (chạy bên trong lock)
     */
    private void performPlaceBid(Auction auction, Bidder bidder, double bidAmount) {
        // Sử dụng logic ném exception đã được "cấy" vào Model Auction
        auction.validateBid(bidAmount);

        // Nếu vượt qua validateBid (không ném exception), tiến hành cập nhật trạng thái
        BidTransaction transaction = new BidTransaction(
                "TX-" + System.currentTimeMillis(),
                bidder,
                bidAmount,
                LocalDateTime.now()
        );

        // Gọi hàm cập nhật dữ liệu nội bộ của đối tượng Auction
        auction.updateAuctionState(bidder, bidAmount, transaction);

        System.out.println("[SUCCESS] " + bidder.getUsername() + " đã đặt giá thành công: " + bidAmount);
    }

    /**
     * CHỨC NĂNG 3: CHUYỂN ĐỔI TRẠNG THÁI (STATUS TRANSITION)
     * Công dụng: Đảm bảo phiên đấu giá tuân thủ đúng vòng đời (OPEN -> RUNNING -> FINISHED).
     */

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
        if (current == next) {
            return true; // Không thay đổi gì
        }

        return switch (current) {
            case OPEN -> (next == AuctionStatus.RUNNING || next == AuctionStatus.CANCELED);
            case RUNNING -> (next == AuctionStatus.FINISHED || next == AuctionStatus.CANCELED);
            case FINISHED -> (next == AuctionStatus.PAID || next == AuctionStatus.CANCELED);
            default -> false; // PAID hoặc CANCELED là trạng thái cuối, không thể chuyển đi tiếp
        };
    }
}