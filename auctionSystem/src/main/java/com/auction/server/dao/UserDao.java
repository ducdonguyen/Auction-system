package com.auction.server.dao;

import com.auction.server.config.DatabaseConfig;
import com.auction.shared.models.AuthUser;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * DAO xử lý các thao tác liên quan đến người dùng trong cơ sở dữ liệu.
 */
public class UserDao {

  public boolean existsByUsernameOrEmail(String username, String email) throws SQLException {
    String sql = "SELECT COUNT(*) FROM users WHERE username = ? OR email = ?";
    try (Connection conn = DatabaseConfig.getConnection();
         PreparedStatement ps = conn.prepareStatement(sql)) {
      ps.setString(1, username);
      ps.setString(2, email);
      try (ResultSet rs = ps.executeQuery()) {
        return rs.next() && rs.getInt(1) > 0;
      }
    }
  }

  public void register(AuthUser user) throws SQLException {
    String sql = "INSERT INTO users (full_name, username, email, password_hash, account_role) VALUES (?, ?, ?, ?, ?)";
    try (Connection conn = DatabaseConfig.getConnection();
         PreparedStatement ps = conn.prepareStatement(sql)) {
      ps.setString(1, user.getFullName());
      ps.setString(2, user.getUsername());
      ps.setString(3, user.getEmail());
      ps.setString(4, user.getPasswordHash());
      ps.setString(5, "USER");
      ps.executeUpdate();
    }
  }

  public AuthUser findByUsername(String username) throws SQLException {
    String sql = "SELECT * FROM users WHERE username = ?";
    try (Connection conn = DatabaseConfig.getConnection();
         PreparedStatement ps = conn.prepareStatement(sql)) {
      ps.setString(1, username);
      try (ResultSet rs = ps.executeQuery()) {
        if (!rs.next()) {
          return null;
        }
        // Gọi constructor 5 tham số của AuthUser, lấy cột account_role
        AuthUser user = new AuthUser(
                rs.getString("full_name"),
                rs.getString("username"),
                rs.getString("email"),
                rs.getString("password_hash"),
                rs.getString("account_role") // Lấy role ("USER" hoặc "ADMIN") từ CSDL
        );
        user.setId(rs.getLong("id"));
        return user;
      }
    }
  }
}
