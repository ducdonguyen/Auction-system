package com.auction.server.config;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

public final class DatabaseConfig {

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
        String sqlCreateTable = """
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

        try (Connection connection = DriverManager.getConnection(DB_URL, USERNAME, PASSWORD);
                Statement statement = connection.createStatement()) {
            statement.execute(sqlCreateTable);
            statement.execute(sqlCreateAuctionsTable);
            System.out.println("[DB] Đã đảm bảo bảng 'users' và 'auctions' tồn tại.");
        }
    }
}
