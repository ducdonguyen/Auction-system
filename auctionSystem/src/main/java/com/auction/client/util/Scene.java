package com.auction.client.util;

/**
 * Định nghĩa tất cả các màn hình trong ứng dụng.
 */
public enum Scene {
    LOGIN("/views/Login.fxml", 980, 640, false, "Đăng nhập"),
    REGISTER("/views/Register.fxml", 980, 640, false, "Đăng ký"),
    AUCTION_LIST("/views/AuctionList.fxml", 1200, 770, true, "Danh sách đấu giá"),
    AUCTION_ROOM("/views/AuctionRoom.fxml", 1180, 780, true, "Phòng đấu giá"),
    ADMIN_DASHBOARD("/views/AdminDashboard.fxml", 1200, 760, true, "Bảng điều khiển Admin"),
    CREATE_AUCTION("/views/Create_auction.fxml", 980, 640, false, "Tạo phiên");

    public final String fxmlPath;
    public final double width;
    public final double height;
    public final boolean isMaximized;
    public final String title;

    Scene(String fxmlPath, double width, double height, boolean isMaximized, String title) {
        this.fxmlPath = fxmlPath;
        this.width = width;
        this.height = height;
        this.isMaximized = isMaximized;
        this.title = title;
    }
}