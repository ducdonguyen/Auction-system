package com.auction.demo.client.dao;
import com.auction.demo.client.config.DatabaseConfig;
import com.auction.shared.models.AuthUser;
import java.sql.*;
public class UserDao {
    public void initializeDatabase() throws SQLException {
        try (Connection conn = DatabaseConfig.getConnection(); Statement stmt = conn.createStatement()) {
            stmt.execute("CREATE TABLE IF NOT EXISTS users (id INT AUTO_INCREMENT PRIMARY KEY, full_name VARCHAR(100), username VARCHAR(50) UNIQUE, email VARCHAR(100) UNIQUE, password_hash VARCHAR(255))");
        }
    }
    public AuthUser findByUsername(String username) throws SQLException {
        try (Connection conn = DatabaseConfig.getConnection(); PreparedStatement ps = conn.prepareStatement("SELECT * FROM users WHERE username = ?")) {
            ps.setString(1, username);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return new AuthUser(rs.getString("full_name"), rs.getString("username"), rs.getString("email"), rs.getString("password_hash"));
        }
        return null;
    }
    public boolean existsByUsernameOrEmail(String u, String e) throws SQLException {
        try (Connection conn = DatabaseConfig.getConnection(); PreparedStatement ps = conn.prepareStatement("SELECT COUNT(*) FROM users WHERE username = ? OR email = ?")) {
            ps.setString(1, u); ps.setString(2, e);
            ResultSet rs = ps.executeQuery();
            return rs.next() && rs.getInt(1) > 0;
        }
    }
    public void register(AuthUser u) throws SQLException {
        try (Connection conn = DatabaseConfig.getConnection(); PreparedStatement ps = conn.prepareStatement("INSERT INTO users (full_name, username, email, password_hash) VALUES (?, ?, ?, ?)")) {
            ps.setString(1, u.getFullName()); ps.setString(2, u.getUsername()); ps.setString(3, u.getEmail()); ps.setString(4, u.getPasswordHash());
            ps.executeUpdate();
        }
    }
}
