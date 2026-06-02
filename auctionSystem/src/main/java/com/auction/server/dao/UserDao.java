package com.auction.server.dao;

import com.auction.server.config.DatabaseConfig;
import com.auction.shared.models.auth.UserAccount;
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

    public void register(UserAccount user) throws SQLException {
        String sql = "INSERT INTO users (full_name, username, email, password_hash, account_role, balance) VALUES (?, ?, ?, ?, ?, ?)";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, user.getFullName());
            ps.setString(2, user.getUsername());
            ps.setString(3, user.getEmail());
            ps.setString(4, user.getPasswordHash());
            ps.setString(5, "USER");
            ps.setDouble(6, user.getBalance()); // Lưu kèm số dư ban đầu (mặc định 0.0)
            ps.executeUpdate();
        }
    }

    public UserAccount findByUsername(String username) throws SQLException {
        String sql = "SELECT * FROM users WHERE username = ?";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, username);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    return null;
                }

                // CẬP NHẬT: Sử dụng Constructor đầy đủ để ánh xạ cột 'balance' từ DB lên Object bộ nhớ
                UserAccount user = new UserAccount(
                        rs.getLong("id"),
                        rs.getString("full_name"),
                        rs.getString("username"),
                        rs.getString("email"),
                        rs.getString("password_hash"),
                        rs.getString("account_role"),
                        rs.getDouble("balance") // ĐỌC THÊM CỘT SỐ DƯ TẠI ĐÂY
                );
                return user;
            }
        }
    }

    /**
     * Xử lý cộng dồn tiền nạp và trả về số dư mới sau khi cập nhật thành công.
     */
    public double updateBalance(String username, double amount) throws SQLException {
        String updateSql = "UPDATE users SET balance = balance + ? WHERE username = ?";
        String selectSql = "SELECT balance FROM users WHERE username = ?";

        try (Connection conn = DatabaseConfig.getConnection()) {
            conn.setAutoCommit(false); // Bật chế độ Transaction để đảm bảo tính toàn vẹn dữ liệu nạp tiền

            // 1. Thực hiện tăng số dư tài khoản
            try (PreparedStatement pStmt = conn.prepareStatement(updateSql)) {
                pStmt.setDouble(1, amount);
                pStmt.setString(2, username);
                pStmt.executeUpdate();
            }

            // 2. Truy vấn lại để lấy chính xác con số balance mới nhất trong Database
            double finalBalance = 0.0;
            try (PreparedStatement pStmt = conn.prepareStatement(selectSql)) {
                pStmt.setString(1, username);
                try (ResultSet rs = pStmt.executeQuery()) {
                    if (rs.next()) {
                        finalBalance = rs.getDouble("balance");
                    }
                }
            }

            conn.commit(); // Hoàn tất phiên giao dịch an toàn
            return finalBalance;

        } catch (SQLException e) {
            throw e; // Ném ngoại lệ để AuthService tầng trên nhận biết và log lỗi
        }
    }

    public double getBalance(String username) throws SQLException {
        String sql = "SELECT balance FROM users WHERE username = ?";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, username);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getDouble("balance");
                }
            }
        }
        return 0.0;
    }

    /**
     * Đóng băng (trừ tiền) khi đặt giá - Sử dụng Transaction để đảm bảo tính nguyên tử.
     * FIX: Thêm transaction management để tránh partial failures
     */
    public boolean freezeBalance(String username, double amount) throws SQLException {
        String sql = "UPDATE users SET balance = balance - ? WHERE username = ? AND balance >= ?";
        try (Connection conn = DatabaseConfig.getConnection()) {
            conn.setAutoCommit(false);  // BẬT TRANSACTION
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setDouble(1, amount);
                ps.setString(2, username);
                ps.setDouble(3, amount);
                int updated = ps.executeUpdate();

                if (updated == 1) {
                    conn.commit();  // COMMIT NẾU THÀNH CÔNG
                    return true;
                } else {
                    conn.rollback();  // ROLLBACK NẾU KHÔNG CẬP NHẬT
                    return false;
                }
            } catch (SQLException e) {
                conn.rollback();  // ROLLBACK NGAY NẾU CÓ EXCEPTION
                throw e;
            }
        }
    }

    /**
     * Hoàn tiền khi bị vượt giá hoặc Admin hủy phiên.
     * FIX: Thêm transaction management để đảm bảo tính nguyên tử
     */
    public void refundBalance(String username, double amount) throws SQLException {
        String sql = "UPDATE users SET balance = balance + ? WHERE username = ?";
        try (Connection conn = DatabaseConfig.getConnection()) {
            conn.setAutoCommit(false);  // BẬT TRANSACTION
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setDouble(1, amount);
                ps.setString(2, username);
                ps.executeUpdate();
                conn.commit();  // COMMIT THÀNH CÔNG
            } catch (SQLException e) {
                conn.rollback();  // ROLLBACK NẾU CÓ EXCEPTION
                throw e;
            }
        }
    }
}