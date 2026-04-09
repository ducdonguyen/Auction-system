package com.auction.demo.client.config;

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

    private static void initializeDatabase() throws SQLException {
        try (Connection connection = DriverManager.getConnection(BASE_URL, USERNAME, PASSWORD);
             Statement statement = connection.createStatement()) {
            statement.execute("CREATE DATABASE IF NOT EXISTS auction_system");
        }
    }
}
