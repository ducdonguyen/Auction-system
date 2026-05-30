package com.auction.server.repository;

import com.auction.server.config.DatabaseConfig;
import com.auction.shared.models.auction.Auction;
import com.auction.shared.models.auction.AuctionStatus;
import com.auction.shared.models.auction.BidTransaction;
import com.auction.shared.models.auth.Bidder;
import com.auction.shared.models.item.Item;
import com.auction.shared.models.auth.Seller;
import com.auction.shared.models.item.ItemFactory;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Repository xử lý lưu trữ dữ liệu phiên đấu giá vào cơ sở dữ liệu.
 */
public class AuctionRepository {
  private static final Logger logger = LoggerFactory.getLogger(AuctionRepository.class);

  // 1. CHUỖI SQL CỐT LÕI: Tự động LEFT JOIN lấy kèm Tên thật (Full Name) của Người Bán và Người Dẫn Đầu
  private static final String BASE_AUCTION_SQL =
          "SELECT a.*, " +
                  "u1.full_name AS seller_full_name, " +
                  "u2.full_name AS bidder_full_name " +
                  "FROM auctions a " +
                  "LEFT JOIN users u1 ON a.seller_username = u1.username " +
                  "LEFT JOIN users u2 ON a.highest_bidder_username = u2.username ";

  public AuctionRepository() {
  }

  public void init() {
    try {
      DatabaseConfig.initializeDatabase();
    } catch (SQLException e) {
      logger.error("[ERROR] DB init failed: {}", e.getMessage());
    }
  }

  /**
   * Hàm phụ trợ phục hồi thông tin Người dẫn đầu (Kèm Tên Thật) cho các danh sách (Sảnh, Admin)
   */
  private void applyHighestBidderForList(Auction auction, ResultSet rs) throws SQLException {
    String bidderUsername = rs.getString("highest_bidder_username");
    if (bidderUsername != null) {
      Bidder b = new Bidder(bidderUsername, "", 0);

      // BƠM TÊN THẬT CHO NGƯỜI DẪN ĐẦU
      String bidderFullName = rs.getString("bidder_full_name");
      if (bidderFullName != null && !bidderFullName.trim().isEmpty()) {
        b.setFullName(bidderFullName);
      }

      BidTransaction txLobby = new BidTransaction("TX-LOBBY", b, auction.getCurrentPrice(), LocalDateTime.now());
      auction.updateAuctionState(b, auction.getCurrentPrice(), txLobby);
    }
  }

  /**
   * Lấy toàn bộ danh sách phòng đấu giá (Dành cho màn hình Sảnh - Lobby)
   */
  public List<Auction> findAll() {
    List<Auction> results = new ArrayList<>();
    String sql = BASE_AUCTION_SQL + "WHERE a.status != 'PENDING'";

    try (Connection connection = DatabaseConfig.getConnection();
         PreparedStatement ps = connection.prepareStatement(sql);
         ResultSet rs = ps.executeQuery()) {
      while (rs.next()) {
        Auction auction = map(rs);
        applyHighestBidderForList(auction, rs); // Phục hồi Người dẫn đầu
        results.add(auction);
      }
    } catch (SQLException e) {
      logger.error("FindAll failed: {}", e.getMessage());
    }
    return results;
  }

  /**
   * Lấy các phòng đang đợi duyệt
   */
  public List<Auction> findPendingAuctions(){
    List <Auction> pending = new ArrayList<>();
    String sql = BASE_AUCTION_SQL + "WHERE a.status = 'PENDING'";

    try (Connection conn = DatabaseConfig.getConnection();
         PreparedStatement stmt = conn.prepareStatement(sql);
         ResultSet rs = stmt.executeQuery()) {
      while (rs.next()) {
        Auction auction = map(rs);
        applyHighestBidderForList(auction, rs); // Phục hồi Người dẫn đầu
        pending.add(auction);
      }
    } catch (SQLException e) {
      logger.error("Lỗi khi lấy danh sách phiên đấu giá đang chờ duyệt: {}", e.getMessage());
    }
    return pending;
  }

  /**
   * Lấy chi tiết phòng đấu giá và toàn bộ lịch sử đặt giá (Dành cho người vào phòng)
   */
  public Auction findById(String auctionId) {
    Auction auction = null;
    String sql = BASE_AUCTION_SQL + "WHERE a.id = ?";

    try (Connection connection = DatabaseConfig.getConnection();
         PreparedStatement preparedStatement = connection.prepareStatement(sql)) {

      preparedStatement.setString(1, auctionId);
      try (ResultSet resultSet = preparedStatement.executeQuery()) {
        if (resultSet.next()) {
          auction = map(resultSet); // Lấy cái vỏ phòng đấu giá
        }
      }

      // NẾU CÓ PHÒNG -> KÉO LỊCH SỬ THẬT TỪ BẢNG bid_transactions (LEFT JOIN LẤY KÈM TÊN THẬT)
      if (auction != null) {
        String sqlBids =
                "SELECT bt.*, u.full_name AS bidder_full_name " +
                        "FROM bid_transactions bt " +
                        "LEFT JOIN users u ON bt.bidder_username = u.username " +
                        "WHERE bt.auction_id = ? ORDER BY bt.bid_time ASC";

        try (PreparedStatement psBids = connection.prepareStatement(sqlBids)) {
          psBids.setString(1, auctionId);
          try (ResultSet rsBids = psBids.executeQuery()) {
            while (rsBids.next()) {
              Bidder b = new Bidder(rsBids.getString("bidder_username"), "", 0);

              // BƠM TÊN THẬT TỪ DATABASE VÀO LỊCH SỬ GIAO DỊCH
              String bidderFullName = rsBids.getString("bidder_full_name");
              if (bidderFullName != null && !bidderFullName.trim().isEmpty()) {
                b.setFullName(bidderFullName);
              }

              BidTransaction tx = new BidTransaction(
                      rsBids.getString("id"),
                      b,
                      rsBids.getDouble("bid_amount"),
                      rsBids.getTimestamp("bid_time").toLocalDateTime()
              );
              auction.updateAuctionState(b, tx.bidAmount(), tx);
            }
          }
        }
      }
    } catch (SQLException e) {
      logger.error("Find failed {}: {}", auctionId, e.getMessage());
    }
    return auction;
  }

  public Auction save(Auction auction) {
    String sql = "INSERT INTO auctions (id, item_id, item_name, item_description, item_starting_price, "
            + "item_type, item_extra_info, seller_username, start_time, end_time, current_price, step_price, "
            + "highest_bidder_username, status) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?) "
            + "ON DUPLICATE KEY UPDATE item_name = VALUES(item_name), item_description = VALUES(item_description), "
            + "item_starting_price = VALUES(item_starting_price), item_type = VALUES(item_type), "
            + "item_extra_info = VALUES(item_extra_info), seller_username = VALUES(seller_username), "
            + "start_time = VALUES(start_time), end_time = VALUES(end_time), "
            + "current_price = VALUES(current_price), step_price = VALUES(step_price), "
            + "highest_bidder_username = VALUES(highest_bidder_username), status = VALUES(status)";

    try (Connection connection = DatabaseConfig.getConnection();
         PreparedStatement preparedStatement = connection.prepareStatement(sql)) {

      preparedStatement.setString(1, auction.getAuctionId());
      Item item = auction.getItem();
      preparedStatement.setString(2, item != null ? item.getId() : null);
      preparedStatement.setString(3, item != null ? item.getName() : null);
      preparedStatement.setString(4, item != null ? item.getDescription() : null);
      preparedStatement.setDouble(5, item != null ? item.getStartingPrice() : 0);
      preparedStatement.setString(6, item != null ? item.getItemType() : "UNKNOWN");
      preparedStatement.setString(7, item != null ? item.getExtraInfo() : "");
      preparedStatement.setString(8, auction.getSeller() != null ? auction.getSeller().getUsername() : null);
      preparedStatement.setTimestamp(9, Timestamp.valueOf(auction.getStartTime()));
      preparedStatement.setTimestamp(10, Timestamp.valueOf(auction.getEndTime()));
      preparedStatement.setDouble(11, auction.getCurrentPrice());
      preparedStatement.setDouble(12, auction.getStepPrice());
      preparedStatement.setString(13, auction.getHighestBidder() != null ? auction.getHighestBidder().getUsername() : null);
      preparedStatement.setString(14, auction.getStatus().name());
      preparedStatement.executeUpdate();

      if (auction.getBidHistory() != null && !auction.getBidHistory().isEmpty()) {
        String sqlBid = "INSERT IGNORE INTO bid_transactions (id, auction_id, bidder_username, bid_amount, bid_time) VALUES (?, ?, ?, ?, ?)";
        try (PreparedStatement psBid = connection.prepareStatement(sqlBid)) {
          for (BidTransaction tx : auction.getBidHistory()) {
            psBid.setString(1, tx.transactionId());
            psBid.setString(2, auction.getAuctionId());
            psBid.setString(3, tx.bidder().getUsername());
            psBid.setDouble(4, tx.bidAmount());
            psBid.setTimestamp(5, Timestamp.valueOf(tx.timestamp()));
            psBid.addBatch();
          }
          psBid.executeBatch();
        }
      }
    } catch (SQLException e) {
      logger.error("Save failed {}: {}", auction.getAuctionId(), e.getMessage());
    }
    return auction;
  }

  public List<Auction> findByStatusAndStartTimeBefore(AuctionStatus status, LocalDateTime time) {
    return findBy(BASE_AUCTION_SQL + "WHERE a.status = ? AND a.start_time <= ?", status.name(), Timestamp.valueOf(time));
  }

  public List<Auction> findByStatusAndEndTimeBefore(AuctionStatus status, LocalDateTime time) {
    return findBy(BASE_AUCTION_SQL + "WHERE a.status = ? AND a.end_time <= ?", status.name(), Timestamp.valueOf(time));
  }

  private List<Auction> findBy(String sql, String param1, Timestamp param2) {
    List<Auction> results = new ArrayList<>();
    try (Connection connection = DatabaseConfig.getConnection();
         PreparedStatement preparedStatement = connection.prepareStatement(sql)) {
      preparedStatement.setString(1, param1);
      preparedStatement.setTimestamp(2, param2);
      try (ResultSet resultSet = preparedStatement.executeQuery()) {
        while (resultSet.next()) {
          Auction auction = map(resultSet);
          applyHighestBidderForList(auction, resultSet);
          results.add(auction);
        }
      }
    } catch (SQLException e) {
      logger.error("Query failed: {}", e.getMessage());
    }
    return results;
  }

  /**
   * Ánh xạ ResultSet thành đối tượng Auction và bơm Tên thật cho Người Bán.
   */
  private Auction map(ResultSet resultSet) throws SQLException {
    String id = resultSet.getString("id");
    String name = resultSet.getString("item_name");
    String desc = resultSet.getString("item_description");
    String type = resultSet.getString("item_type");
    String extra = resultSet.getString("item_extra_info");
    double startingPrice = resultSet.getDouble("item_starting_price");

    Item item = ItemFactory.createItem(type, name, desc, startingPrice, extra);

    Seller seller = new Seller(resultSet.getString("seller_username"), "");

    // 2. BƠM TÊN THẬT CHO NGƯỜI BÁN TẠI ĐÂY
    String sellerFullName = resultSet.getString("seller_full_name");
    if (sellerFullName != null && !sellerFullName.trim().isEmpty()) {
      seller.setFullName(sellerFullName);
    }

    Auction auction = new Auction(id, item, seller,
            resultSet.getDouble("current_price"), resultSet.getDouble("step_price"),
            resultSet.getTimestamp("start_time").toLocalDateTime(), resultSet.getTimestamp("end_time").toLocalDateTime());

    auction.setStatus(AuctionStatus.valueOf(resultSet.getString("status")));

    return auction;
  }

}