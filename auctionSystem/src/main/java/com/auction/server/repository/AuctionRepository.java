package com.auction.server.repository;

import com.auction.server.config.DatabaseConfig;
import com.auction.shared.models.Art;
import com.auction.shared.models.Auction;
import com.auction.shared.models.AuctionStatus;
import com.auction.shared.models.Bidder;
import com.auction.shared.models.Electronics;
import com.auction.shared.models.Item;
import com.auction.shared.models.Seller;
import com.auction.shared.models.Vehicle;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class AuctionRepository {

    public AuctionRepository() {
        try {
            DatabaseConfig.initializeDatabase();
        } catch (SQLException e) {
            System.err.println("[ERROR] Không thể khởi tạo database: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public Auction findById(String auctionId) {
        String sql = "SELECT * FROM auctions WHERE id = ?";
        try (Connection conn = DatabaseConfig.getConnection();
                PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, auctionId);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return mapResultSetToAuction(rs);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * Nhiệm vụ: Lưu mới (INSERT) hoặc cập nhật (UPDATE) thông tin vào MySQL.
     */
    public Auction save(Auction auction) {
        String sql = "INSERT INTO auctions (id, item_id, item_name, item_description, item_starting_price, " +
                "item_type, item_extra_info, seller_username, start_time, end_time, current_price, step_price, " +
                "highest_bidder_username, status) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?) " +
                "ON DUPLICATE KEY UPDATE item_name = VALUES(item_name), item_description = VALUES(item_description), " +
                "item_starting_price = VALUES(item_starting_price), item_type = VALUES(item_type), " +
                "item_extra_info = VALUES(item_extra_info), seller_username = VALUES(seller_username), " +
                "start_time = VALUES(start_time), end_time = VALUES(end_time), " +
                "current_price = VALUES(current_price), step_price = VALUES(step_price), " +
                "highest_bidder_username = VALUES(highest_bidder_username), status = VALUES(status)";

        try (Connection conn = DatabaseConfig.getConnection();
                PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, auction.getAuctionId());
            Item item = auction.getItem();
            pstmt.setString(2, item != null ? item.getId() : null);
            pstmt.setString(3, item != null ? item.getName() : null);
            pstmt.setString(4, item != null ? item.getDescription() : null);
            pstmt.setDouble(5, item != null ? item.getStartingPrice() : 0);

            String itemType = (item != null) ? item.getItemType() : "UNKNOWN";
            String extraInfo = (item != null) ? item.getExtraInfo() : "";

            pstmt.setString(6, itemType);
            pstmt.setString(7, extraInfo);

            pstmt.setString(8, auction.getSeller() != null ? auction.getSeller().getUsername() : null);
            pstmt.setTimestamp(9, Timestamp.valueOf(auction.getStartTime()));
            pstmt.setTimestamp(10, Timestamp.valueOf(auction.getEndTime()));
            pstmt.setDouble(11, auction.getCurrentPrice());
            pstmt.setDouble(12, auction.getStepPrice());
            pstmt.setString(13, auction.getHighestBidder() != null ? auction.getHighestBidder().getUsername() : null);
            pstmt.setString(14, auction.getStatus().name());

            pstmt.executeUpdate();
            System.out.println("[DB] Đã lưu/cập nhật phiên đấu giá " + auction.getAuctionId());
        } catch (SQLException e) {
            e.printStackTrace();
        }


    }

    /**
     * Tìm các phiên đang OPEN nhưng đã đến giờ START (Dùng cho Scheduler)
     */
    public List<Auction> findByStatusAndStartTimeBefore(AuctionStatus status, LocalDateTime time) {
        List<Auction> result = new ArrayList<>();
        String sql = "SELECT * FROM auctions WHERE status = ? AND start_time <= ?";
        try (Connection conn = DatabaseConfig.getConnection();
                PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, status.name());
            pstmt.setTimestamp(2, Timestamp.valueOf(time));
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    result.add(mapResultSetToAuction(rs));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return result;
    }

    /**
     * Tìm các phiên đang RUNNING nhưng đã đến giờ END (Dùng cho Scheduler)
     */
    public List<Auction> findByStatusAndEndTimeBefore(AuctionStatus status, LocalDateTime time) {
        List<Auction> result = new ArrayList<>();
        String sql = "SELECT * FROM auctions WHERE status = ? AND end_time <= ?";
        try (Connection conn = DatabaseConfig.getConnection();
                PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, status.name());
            pstmt.setTimestamp(2, Timestamp.valueOf(time));
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    result.add(mapResultSetToAuction(rs));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return result;
    }

    private Auction mapResultSetToAuction(ResultSet rs) throws SQLException {
        String id = rs.getString("id");
        String itemName = rs.getString("item_name");
        String itemDescription = rs.getString("item_description");
        double itemStartingPrice = rs.getDouble("item_starting_price");
        String itemType = rs.getString("item_type");
        String extraInfo = rs.getString("item_extra_info");

        Item item = switch (itemType) {
            case "ART" -> new Art(itemName, itemDescription, itemStartingPrice, extraInfo);

            case "ELECTRONICS" ->
                    new Electronics(itemName, itemDescription, itemStartingPrice, Integer.parseInt(extraInfo));

            case "VEHICLE" -> new Vehicle(itemName, itemDescription, itemStartingPrice, extraInfo);

            default -> throw new IllegalArgumentException("Loại hàng hóa không hợp lệ: " + itemType);
        };
        // Lưu ý: item_id từ DB có thể khác id sinh tự động trong constructor Item (Entity), 
        // nhưng hiện tại cấu trúc Entity/Item không cho phép set ID dễ dàng qua constructor.

        String sellerUsername = rs.getString("seller_username");
        Seller seller = new Seller(sellerUsername, ""); // Mật khẩu trống vì không cần ở đây

        LocalDateTime startTime = rs.getTimestamp("start_time").toLocalDateTime();
        LocalDateTime endTime = rs.getTimestamp("end_time").toLocalDateTime();
        double currentPrice = rs.getDouble("current_price");
        double stepPrice = rs.getDouble("step_price");
        String statusStr = rs.getString("status");

        Auction auction = new Auction(id, item, seller, currentPrice, stepPrice, startTime, endTime);
        auction.setStatus(AuctionStatus.valueOf(statusStr));

        String bidderUsername = rs.getString("highest_bidder_username");
        if (bidderUsername != null) {
            auction.updateAuctionState(new Bidder(bidderUsername, "", 0), currentPrice, null);
            // Lưu ý: BidTransaction history chưa được load từ DB trong ví dụ này.
        }

        return auction;
    }
}