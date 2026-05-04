package com.auction.server.repository;
import com.auction.server.config.DatabaseConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.auction.shared.models.*;
import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
public class AuctionRepository {
    private static final Logger logger = LoggerFactory.getLogger(AuctionRepository.class);
    public AuctionRepository() {}
    public void init() {
        try { DatabaseConfig.initializeDatabase(); } catch (SQLException e) { logger.error("[ERROR] DB init failed: {}", e.getMessage()); }
    }
    public Auction findById(String auctionId) {
        String sql = "SELECT * FROM auctions WHERE id = ?";
        try (Connection connection = DatabaseConfig.getConnection(); PreparedStatement preparedStatement = connection.prepareStatement(sql)) {
            preparedStatement.setString(1, auctionId);
            try (ResultSet resultSet = preparedStatement.executeQuery()) { if (resultSet.next()) return map(resultSet); }
        } catch (SQLException e) { logger.error("Find failed {}: {}", auctionId, e.getMessage()); }
        return null;
    }
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
        try (Connection connection = DatabaseConfig.getConnection(); PreparedStatement preparedStatement = connection.prepareStatement(sql)) {
            preparedStatement.setString(1, auction.getAuctionId()); Item item = auction.getItem();
            preparedStatement.setString(2, item != null ? item.getId() : null); preparedStatement.setString(3, item != null ? item.getName() : null);
            preparedStatement.setString(4, item != null ? item.getDescription() : null); preparedStatement.setDouble(5, item != null ? item.getStartingPrice() : 0);
            preparedStatement.setString(6, item != null ? item.getItemType() : "UNKNOWN"); preparedStatement.setString(7, item != null ? item.getExtraInfo() : "");
            preparedStatement.setString(8, auction.getSeller() != null ? auction.getSeller().getUsername() : null);
            preparedStatement.setTimestamp(9, Timestamp.valueOf(auction.getStartTime())); preparedStatement.setTimestamp(10, Timestamp.valueOf(auction.getEndTime()));
            preparedStatement.setDouble(11, auction.getCurrentPrice()); preparedStatement.setDouble(12, auction.getStepPrice());
            preparedStatement.setString(13, auction.getHighestBidder() != null ? auction.getHighestBidder().getUsername() : null);
            preparedStatement.setString(14, auction.getStatus().name()); preparedStatement.executeUpdate();
        } catch (SQLException e) { logger.error("Save failed {}: {}", auction.getAuctionId(), e.getMessage()); }
        return auction;
    }
    public List<Auction> findByStatusAndStartTimeBefore(AuctionStatus status, LocalDateTime time) {
        return findBy("SELECT * FROM auctions WHERE status = ? AND start_time <= ?", status.name(), Timestamp.valueOf(time));
    }
    public List<Auction> findByStatusAndEndTimeBefore(AuctionStatus status, LocalDateTime time) {
        return findBy("SELECT * FROM auctions WHERE status = ? AND end_time <= ?", status.name(), Timestamp.valueOf(time));
    }
    private List<Auction> findBy(String sql, String param1, Timestamp param2) {
        List<Auction> results = new ArrayList<>();
        try (Connection connection = DatabaseConfig.getConnection(); PreparedStatement preparedStatement = connection.prepareStatement(sql)) {
            preparedStatement.setString(1, param1); preparedStatement.setTimestamp(2, param2);
            try (ResultSet resultSet = preparedStatement.executeQuery()) { while (resultSet.next()) results.add(map(resultSet)); }
        } catch (SQLException e) { logger.error("Query failed: {}", e.getMessage()); }
        return results;
    }
    private Auction map(ResultSet resultSet) throws SQLException {
        String id = resultSet.getString("id"), name = resultSet.getString("item_name"), desc = resultSet.getString("item_description"), 
               type = resultSet.getString("item_type"), extra = resultSet.getString("item_extra_info");
        double startingPrice = resultSet.getDouble("item_starting_price");
        Item item = switch (type) {
            case "ART" -> new Art(name, desc, startingPrice, extra);
            case "ELECTRONICS" -> new Electronics(name, desc, startingPrice, Integer.parseInt(extra));
            case "VEHICLE" -> new Vehicle(name, desc, startingPrice, extra);
            default -> throw new IllegalArgumentException("Invalid type: " + type);
        };
        Auction auction = new Auction(id, item, new Seller(resultSet.getString("seller_username"), ""), 
                resultSet.getDouble("current_price"), resultSet.getDouble("step_price"), 
                resultSet.getTimestamp("start_time").toLocalDateTime(), resultSet.getTimestamp("end_time").toLocalDateTime());
        auction.setStatus(AuctionStatus.valueOf(resultSet.getString("status")));
        String bidderUsername = resultSet.getString("highest_bidder_username");
        if (bidderUsername != null) auction.updateAuctionState(new Bidder(bidderUsername, "", 0), auction.getCurrentPrice(), null);
        return auction;
    }
}