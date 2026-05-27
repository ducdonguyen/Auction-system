package com.auction.server.config;

import java.sql.*;
import org.mindrot.jbcrypt.BCrypt;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Cấu hình cơ sở dữ liệu cho hệ thống đấu giá kết nối qua Aiven Cloud.
 */
public final class DatabaseConfig {

    private static final Logger logger = LoggerFactory.getLogger(DatabaseConfig.class);

    // Cập nhật URL kết nối tới Aiven. Bắt buộc giữ tham số ssl-mode=REQUIRED ở cuối.
    private static final String DB_URL = "jdbc:mysql://auction-system-ducdo252322-9887.c.aivencloud.com:22336/defaultdb"
            + "?ssl-mode=REQUIRED&serverTimezone=UTC&allowPublicKeyRetrieval=true";

    // BẢO MẬT: Ưu tiên lấy từ biến môi trường hệ thống, nếu không có sẽ lấy mặc định của Aiven
    private static final String USERNAME = System.getenv("DB_USER") != null
            ? System.getenv("DB_USER") : "avnadmin"; // User mặc định của Aiven là avnadmin

    private static final String PASSWORD = System.getenv("DB_PASSWORD") != null
            ? System.getenv("DB_PASSWORD") : "AVNS_6mwRR35RDHdCxUz0YT-"; // <-- THAY MẬT KHẨU CỦA BẠN VÀO ĐÂY

    // ĐA LUỒNG: Từ khóa volatile đảm bảo các luồng luôn đọc được giá trị mới nhất từ Main Memory
    private static volatile boolean initialized = false;

    // Khóa Lock dành cho Double-check locking
    private static final Object LOCK = new Object();

    private DatabaseConfig() {
        // Ngăn chặn khởi tạo object từ bên ngoài
    }

    /**
     * Lấy kết nối đến Database an toàn trong môi trường đa luồng.
     */
    public static Connection getConnection() throws SQLException {
        // Kiểm tra lần 1 (Không khóa để tối ưu hiệu suất)
        if (!initialized) {
            // Chỉ những luồng chạy vào đây lúc DB chưa khởi tạo mới phải xếp hàng (Lock)
            synchronized (LOCK) {
                // Kiểm tra lần 2 (Đảm bảo luồng vào sau không chạy lại hàm khởi tạo)
                if (!initialized) {
                    initializeDatabase();
                    initialized = true;
                }
            }
        }
        return DriverManager.getConnection(DB_URL, USERNAME, PASSWORD);
    }

    /**
     * Khởi tạo cấu trúc cơ sở dữ liệu và dữ liệu mặc định trực tiếp trên defaultdb của Aiven.
     */
    public static void initializeDatabase() throws SQLException {
        // BỎ HOÀN TOÀN khối try-catch thực thi "CREATE DATABASE IF NOT EXISTS" cũ
        // vì tài khoản trên Cloud không được phép tạo database mới tự do.

        String sqlCreateUsersTable = """
        CREATE TABLE IF NOT EXISTS users (
            id BIGINT PRIMARY KEY AUTO_INCREMENT,
            full_name VARCHAR(100) NOT NULL,
            username VARCHAR(50) NOT NULL UNIQUE,
            email VARCHAR(100) NOT NULL UNIQUE,
            password_hash VARCHAR(255) NOT NULL,
            account_role VARCHAR(20) DEFAULT 'USER',
            balance DOUBLE DEFAULT 0.0, 
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

        String insertAdminSQL = """
            INSERT IGNORE INTO users (full_name, username, password_hash, email, account_role) 
                   VALUES ('nguyen_admin', 'admin', ?, 'admin@gmail.com', 'ADMIN')
            """;

        // Kết nối thẳng tới DB_URL (tức là defaultdb) để khởi tạo bảng dữ liệu
        try (Connection connection = DriverManager.getConnection(DB_URL, USERNAME, PASSWORD)) {
            try (Statement statement = connection.createStatement()) {
                statement.execute(sqlCreateUsersTable);
                statement.execute(sqlCreateAuctionsTable);
                statement.execute(sqlCreateBidTransactionsTable);
            }

            // Dùng PreparedStatement riêng cho việc chèn Admin để truyền mật khẩu đã hash an toàn
            try (PreparedStatement ps = connection.prepareStatement(insertAdminSQL)) {
                ps.setString(1, BCrypt.hashpw("admin123", BCrypt.gensalt())); // Đẩy chuỗi đã băm vào dấu ?
                ps.executeUpdate();
            }
        }
        logger.info("[DB] Đã kết nối Cloud thành công. Đảm bảo cấu trúc bảng 'users', 'auctions' và 'bid_transactions' sẵn sàng.");
    }
}