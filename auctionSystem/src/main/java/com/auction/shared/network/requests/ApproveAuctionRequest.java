package com.auction.shared.network.requests;

import java.io.Serializable;

/**
 * Lớp Đóng gói dữ liệu (DTO) gửi yêu cầu phê duyệt một phiên đấu giá từ Admin lên Server.
 * Lớp này bắt buộc phải implements Serializable để truyền được qua Socket.
 */
public class ApproveAuctionRequest implements Serializable {
    private static final long serialVersionUID = 1L;

    private String auctionId;

    /**
     * Constructor mặc định (Không đối số) - Cần thiết cho thư viện sê-ri hóa
     */
    public ApproveAuctionRequest() {
    }

    /**
     * Constructor đầy đủ tham số để Admin truyền ID phòng vào.
     */
    public ApproveAuctionRequest(String auctionId) {
        this.auctionId = auctionId;
    }

    public String getAuctionId() {
        return auctionId;
    }

    public void setAuctionId(String auctionId) {
        this.auctionId = auctionId;
    }

    /**
     * Hàm toString hỗ trợ việc ghi Log ở cả phía Client và Server khi debug.
     */
    @Override
    public String toString() {
        return "ApproveAuctionRequest{" +
                "auctionId='" + auctionId + '\'' +
                '}';
    }
}