package com.auction.shared.models;

public enum AuctionStatus {
    OPEN,       // Vừa tạo, chờ bắt đầu
    RUNNING,    // Đang trong thời gian đấu giá
    FINISHED,   // Đã hết giờ, chờ thanh toán
    PAID,       // Hoàn thành giao dịch
    CANCELED    // Hủy (do không có người mua hoặc lỗi)
}