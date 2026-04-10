package com.auction.server.core;

import com.auction.server.concurrency.AuctionLockManager;
import com.auction.shared.models.*;
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

    /**
     * CHỨC NĂNG 1: TẠO PHIÊN ĐẤU GIÁ MỚI
     * Công dụng: Khởi tạo một phiên đấu giá và đưa nó vào hệ thống.
     */
    public Auction createAuction(Item item, Seller seller, double startingPrice, double stepPrice) {
        // Tạo một ID duy nhất bằng UUID (tránh trùng lặp ID phiên)
        String auctionId = "AUC-" + UUID.randomUUID().toString().substring(0, 8);

        // Khởi tạo đối tượng Auction
        Auction newAuction = new Auction(auctionId, item, seller, startingPrice, stepPrice);

        // Mặc định phiên mới tạo sẽ ở trạng thái OPEN
        newAuction.setStatus("OPEN");

        System.out.println("[INFO] Đã tạo phiên đấu giá mới: " + auctionId + " cho mặt hàng " + item.getName());
        return newAuction;
    }

    /**
     * CHỨC NĂNG 2: ĐẶT GIÁ (PLACE BID)
     * Công dụng: Xử lý tranh chấp và cập nhật giá hiện tại.
     * Sử dụng AuctionLockManager để lock từng phiên đấu giá, đảm bảo thread-safe.
     */
    public boolean placeBid(Auction auction, Bidder bidder, double bidAmount) {
        final boolean[] success = {false};

        // Sử dụng lock manager để đảm bảo chỉ một người có thể đặt giá cho phiên này tại một thời điểm
        lockManager.executeWithLock(auction.getAuctionId(), () -> {
            success[0] = performPlaceBid(auction, bidder, bidAmount);
        });

        return success[0];
    }

    /**
     * Xử lý logic đặt giá (chạy bên trong lock)
     */
    private boolean performPlaceBid(Auction auction, Bidder bidder, double bidAmount) {
        // 1. Kiểm tra trạng thái phiên đấu giá (Phải đang RUNNING mới được đặt)
        // Lưu ý: Tạm thời bạn có thể kiểm tra khác FINISHED là được
        if ("FINISHED".equals(auction.getStatus())) {
            System.out.println("[FAILED] Đấu giá đã kết thúc. Người dùng: " + bidder.getUsername());
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
                "TX-" + System.currentTimeMillis(), // Tạo mã giao dịch tạm thời dựa trên thời gian
                bidder,
                bidAmount,
                LocalDateTime.now()
        );

        // Gọi hàm cập nhật dữ liệu nội bộ của đối tượng Auction
        auction.updateAuctionState(bidder, bidAmount, transaction);

        System.out.println("[SUCCESS] " + bidder.getUsername() + " đã đặt giá thành công: " + bidAmount);
        return true;
    }
}