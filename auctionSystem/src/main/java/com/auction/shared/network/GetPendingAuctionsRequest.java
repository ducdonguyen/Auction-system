package com.auction.shared.network;

import java.io.Serializable;

/**
 * Lớp Đóng gói dữ liệu (DTO) gửi tín hiệu yêu cầu lấy danh sách phiên đấu giá chờ duyệt.
 * Lớp này bắt buộc phải implements Serializable để truyền được qua Socket.
 */
public class GetPendingAuctionsRequest implements Serializable {
    private static final long serialVersionUID = 1L;

    /**
     * Constructor mặc định - Vì đây là gói tin yêu cầu lấy danh sách,
     * ta không cần truyền tham số nào cả.
     */
    public GetPendingAuctionsRequest() {
    }

    /**
     * Hàm toString hỗ trợ việc ghi Log ở cả phía Client và Server khi debug.
     */
    @Override
    public String toString() {
        return "GetPendingAuctionsRequest{ Yêu cầu lấy danh sách chờ duyệt }";
    }
}
