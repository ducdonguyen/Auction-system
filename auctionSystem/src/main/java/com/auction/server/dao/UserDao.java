package com.auction.server.dao;
import com.auction.server.config.DatabaseConfig;
import com.auction.shared.models.AuthUser;
import java.sql.*;
public class UserDao {
    public boolean existsByUsernameOrEmail(String username, String email) throws SQLException {
        String sql = "SELECT COUNT(*) FROM users WHERE username = ? OR email = ?";
        try (Connection conn = DatabaseConfig.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, username); ps.setString(2, email);
            try (ResultSet rs = ps.executeQuery()) { return rs.next() && rs.getInt(1) > 0; }
        }
    }
    public void register(AuthUser user) throws SQLException {
        String sql = "INSERT INTO users (full_name, username, email, password_hash) VALUES (?, ?, ?, ?)";
        try (Connection conn = DatabaseConfig.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, user.getFullName()); ps.setString(2, user.getUsername());
            ps.setString(3, user.getEmail()); ps.setString(4, user.getPasswordHash());
            ps.executeUpdate();
        }
    }
    public AuthUser findByUsername(String username) throws SQLException {
        String sql = "SELECT * FROM users WHERE username = ?";
        try (Connection conn = DatabaseConfig.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, username);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) return null;
                AuthUser user = new AuthUser(rs.getString("full_name"), rs.getString("username"), rs.getString("email"), rs.getString("password_hash"));
                user.setId(rs.getLong("id"));
                return user;
            }
        }
    }
}
