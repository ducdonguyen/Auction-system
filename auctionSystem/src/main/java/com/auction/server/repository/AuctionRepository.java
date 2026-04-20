package com.auction.server.repository;

import com.auction.shared.models.Auction;
import com.auction.shared.models.AuctionStatus;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class AuctionRepository {

    /**
     * Nhiệm vụ: Lưu mới (INSERT) hoặc cập nhật (UPDATE) thông tin vào MySQL.
     */
    public Auction save(Auction auction) {
        // TODO (Dành cho thành viên làm Database):
        // Viết câu lệnh SQL ở đây. Sử dụng "INSERT ... ON DUPLICATE KEY UPDATE"
        /*
        String sql = "INSERT INTO auctions (id, status, current_price) VALUES (?, ?, ?) " +
                     "ON DUPLICATE KEY UPDATE status = VALUES(status), current_price = VALUES(current_price)";

        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            // Map dữ liệu từ Object Auction vào câu SQL
            pstmt.setString(1, auction.getAuctionId());
            pstmt.setString(2, auction.getStatus().name());
            pstmt.setDouble(3, auction.getCurrentPrice());

            pstmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        */

        // Tạm thời in ra màn hình để chạy giả lập không lỗi đỏ
        System.out.println(
                "[DB_SIMULATION] Đã lưu/cập nhật phiên đấu giá " + auction.getAuctionId() + " vào Database.");

        // Trả về chính nó để AuctionService có thể gán lại nếu cần
        return auction;
    }

    /**
     * Tìm các phiên đang OPEN nhưng đã đến giờ START (Dùng cho Scheduler)
     */
    public List<Auction> findByStatusAndStartTimeBefore(AuctionStatus status, LocalDateTime time) {
        List<Auction> result = new ArrayList<>();
        // TODO: Viết câu lệnh SELECT * FROM auctions WHERE status = ? AND start_time < ?
        return result;
    }

    /**
     * Tìm các phiên đang RUNNING nhưng đã đến giờ END (Dùng cho Scheduler)
     */
    public List<Auction> findByStatusAndEndTimeBefore(AuctionStatus status, LocalDateTime time) {
        List<Auction> result = new ArrayList<>();
        // TODO: Viết câu lệnh SELECT * FROM auctions WHERE status = ? AND end_time < ?
        return result;
    }
}