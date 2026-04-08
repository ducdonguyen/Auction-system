package com.auction.demo.server;

import com.auction.demo.common.model.*;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Lớp dịch vụ điều phối các hoạt động của phiên đấu giá.
 * Đây là nơi tập trung logic nghiệp vụ và xử lý tranh chấp đa luồng.
 */
public class AuctionService {

    /**
     * Xử lý đặt giá cho một phiên đấu giá cụ thể.
     * Sử dụng từ khóa 'synchronized' để đảm bảo tính Thread-safe.
     * * TẠI SAO DÙNG SYNCHRONIZED?
     * Khi có 2 người cùng đặt giá tại cùng 1 phần nghìn giây, từ khóa này sẽ tạo ra
     * một cơ chế "xếp hàng". Luồng vào trước sẽ giữ khóa (lock), luồng vào sau phải
     * đợi luồng trước xử lý xong mới được kiểm tra giá. Điều này loại bỏ hoàn toàn
     * tình trạng "Lost Update" (cập nhật đè lên nhau).
     *
     * @param auction Đối tượng phiên đấu giá đang xét
     * @param bidder Người tham gia đặt giá
     * @param bidAmount Số tiền đặt giá
     * @return true nếu đặt giá thành công, false nếu không hợp lệ
     */


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
     * (Giữ nguyên đoạn code synchronized bạn đã viết ở trên)
     */
    public synchronized boolean placeBid(Auction auction, Bidder bidder, double bidAmount) {

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