package com.auction.server.dao;

import com.auction.server.config.DatabaseConfig;
import com.auction.shared.models.AuthUser;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class UserDao {

    private static final String SQL_CHECK_EXISTS = "SELECT COUNT(*) FROM users WHERE username = ? OR email = ?";
    private static final String SQL_INSERT_USER = "INSERT INTO users (full_name, username, email, password_hash) " +
            "VALUES (?, ?, ?, ?)";
    private static final String SQL_FIND_USER = "SELECT id, full_name, username, email, password_hash " +
            "FROM users WHERE username = ?";

    public void initializeDatabase() throws SQLException {
        String sql = "CREATE TABLE IF NOT EXISTS users (" +
                "id INT AUTO_INCREMENT PRIMARY KEY, " +
                "full_name VARCHAR(100), " +
                "username VARCHAR(50) UNIQUE, " +
                "email VARCHAR(100) UNIQUE, " +
                "password_hash VARCHAR(255))";
        try (Connection conn = DatabaseConfig.getConnection();
             java.sql.Statement stmt = conn.createStatement()) {
            stmt.execute(sql);
        }
    }

    public boolean existsByUsernameOrEmail(String username, String email) throws SQLException {
        try (Connection connection = DatabaseConfig.getConnection();
                PreparedStatement statement = connection.prepareStatement(SQL_CHECK_EXISTS)) {

            statement.setString(1, username);
            statement.setString(2, email);

            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return resultSet.getInt(1) > 0;
                }
                return false;
            }
        }
    }

    public void register(AuthUser user) throws SQLException {
        try (Connection connection = DatabaseConfig.getConnection();
                PreparedStatement statement = connection.prepareStatement(SQL_INSERT_USER)) {

            statement.setString(1, user.getFullName());
            statement.setString(2, user.getUsername());
            statement.setString(3, user.getEmail());
            statement.setString(4, user.getPasswordHash());
            statement.executeUpdate();
        }
    }

    public AuthUser findByUsername(String username) throws SQLException {
        try (Connection connection = DatabaseConfig.getConnection();
                PreparedStatement statement = connection.prepareStatement(SQL_FIND_USER)) {

            statement.setString(1, username);

            try (ResultSet resultSet = statement.executeQuery()) {
                if (!resultSet.next()) {
                    return null;
                }

                AuthUser user = new AuthUser();
                user.setId(resultSet.getLong("id"));
                user.setFullName(resultSet.getString("full_name"));
                user.setUsername(resultSet.getString("username"));
                user.setEmail(resultSet.getString("email"));
                user.setPasswordHash(resultSet.getString("password_hash"));
                return user;
            }
        }
    }
}