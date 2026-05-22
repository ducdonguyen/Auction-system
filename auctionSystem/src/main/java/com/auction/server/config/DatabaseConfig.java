package com.auction.server.config;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Cấu hình cơ sở dữ liệu cho hệ thống đấu giá.
 */
public final class DatabaseConfig {

  private static final Logger logger = LoggerFactory.getLogger(DatabaseConfig.class);
  private static final String BASE_URL = "jdbc:mysql://localhost:3306"
      + "?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true";
  private static final String DB_URL = "jdbc:mysql://localhost:3306/auction_system"
      + "?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true";
  private static final String USERNAME = "root";
  private static final String PASSWORD = "123456";
  private static boolean initialized = false;

  private DatabaseConfig() {
  }

  public static Connection getConnection() throws SQLException {
    if (!initialized) {
      initializeDatabase();
      initialized = true;
    }
    return DriverManager.getConnection(DB_URL, USERNAME, PASSWORD);
  }

  public static void initializeDatabase() throws SQLException {
    try (Connection connection = DriverManager.getConnection(BASE_URL, USERNAME, PASSWORD);
         Statement statement = connection.createStatement()) {
      statement.execute("CREATE DATABASE IF NOT EXISTS auction_system");
    }
    String sqlCreateUsersTable = """
        CREATE TABLE IF NOT EXISTS users (
            id BIGINT PRIMARY KEY AUTO_INCREMENT,
            full_name VARCHAR(100) NOT NULL,
            username VARCHAR(50) NOT NULL UNIQUE,
            email VARCHAR(100) NOT NULL UNIQUE,
            password_hash VARCHAR(255) NOT NULL,
            created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
        )
        """;

    String sqlCreateAuctionsTable = """
        CREATE TABLE IF NOT EXISTS auctions (
            id VARCHAR(50) PRIMARY KEY,
            item_id VARCHAR(50),
            item_name VARCHAR(100),
            item_description TEXT,
            item_starting_price DOUBLE,
            item_type VARCHAR(20),
            item_extra_info VARCHAR(100),
            seller_username VARCHAR(50),
            start_time DATETIME,
            end_time DATETIME,
            current_price DOUBLE,
            step_price DOUBLE,
            highest_bidder_username VARCHAR(50),
            status VARCHAR(20)
        )
        """;

    String sqlCreateBidTransactionsTable = """
        CREATE TABLE IF NOT EXISTS bid_transactions (
            id VARCHAR(50) PRIMARY KEY,
            auction_id VARCHAR(50) NOT NULL,
            bidder_username VARCHAR(50) NOT NULL,
            bid_amount DOUBLE NOT NULL,
            bid_time DATETIME NOT NULL,
            FOREIGN KEY (auction_id) REFERENCES auctions(id) ON DELETE CASCADE
        )
        """;

    try (Connection connection = DriverManager.getConnection(DB_URL, USERNAME, PASSWORD);
         Statement statement = connection.createStatement()) {
      statement.execute(sqlCreateUsersTable);
      statement.execute(sqlCreateAuctionsTable);
      statement.execute(sqlCreateBidTransactionsTable);
      logger.info("[DB] Đã đảm bảo bảng 'users', 'auctions' và 'bid_transactions' tồn tại.");
    }
  }
}
