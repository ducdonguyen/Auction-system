package com.auction.server.dao;

import com.auction.server.config.DatabaseConfig;
import com.auction.shared.models.AuthUser;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class UserDao {

    public boolean existsByUsernameOrEmail(String username, String email) throws SQLException {
        String sql = "SELECT COUNT(*) FROM users WHERE username = ? OR email = ?";

        try (Connection connection = DatabaseConfig.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql)) {
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
        String sql = "INSERT INTO users (full_name, username, email, password_hash) VALUES (?, ?, ?, ?)";

        try (Connection connection = DatabaseConfig.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, user.getFullName());
            statement.setString(2, user.getUsername());
            statement.setString(3, user.getEmail());
            statement.setString(4, user.getPasswordHash());
            statement.executeUpdate();
        }
    }

    public AuthUser findByUsername(String username) throws SQLException {
        String sql = "SELECT id, full_name, username, email, password_hash FROM users WHERE username = ?";

        try (Connection connection = DatabaseConfig.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql)) {
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
