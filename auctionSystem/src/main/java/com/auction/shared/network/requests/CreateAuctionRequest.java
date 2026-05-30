package com.auction.shared.network.requests;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * Lớp Đóng gói dữ liệu (DTO) gửi yêu cầu tạo phiên đấu giá mới từ Client lên Server.
 * Lớp này bắt buộc phải implements Serializable để truyền được qua Socket.
 */
public class CreateAuctionRequest implements Serializable {
    // Đảm bảo mã định danh phiên bản giống nhau giữa Client và Server để tránh lỗi sê-ri hóa
    private static final long serialVersionUID = 1L;

    private String productName;
    private String description;
    private double startingPrice;
    private double priceStep;
    private String productType;
    private String extraInfo;     // VD: Nếu là Electronics thì là thời gian bảo hành, Art thì là tên tác giả, v.v
    private String sellerUsername;
    private LocalDateTime startTime;
    private LocalDateTime endTime;

    /**
     * Constructor mặc định (Không đối số) - Cần thiết cho một số thư viện sê-ri hóa
     */
    public CreateAuctionRequest() {
    }

    /**
     * Constructor đầy đủ tham số để khởi tạo nhanh từ Form nhập liệu.
     */
    public CreateAuctionRequest(String productName, String description, double startingPrice, double priceStep,
                                String productType, String extraInfo, String sellerUsername,
                                LocalDateTime startTime, LocalDateTime endTime) {
        this.productName = productName;
        this.description = description;
        this.startingPrice = startingPrice;
        this.priceStep = priceStep;
        this.productType = productType;
        this.extraInfo = extraInfo;
        this.sellerUsername = sellerUsername;
        this.startTime = startTime;
        this.endTime = endTime;
    }

    // ==========================================
    // GETTERS VÀ SETTERS
    // ==========================================

    public String getProductName() {
        return productName;
    }

    public void setProductName(String productName) {
        this.productName = productName;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public double getStartingPrice() {
        return startingPrice;
    }

    public void setStartingPrice(double startingPrice) {
        this.startingPrice = startingPrice;
    }

    public double getPriceStep() {
        return priceStep;
    }

    public void setPriceStep(double priceStep) {
        this.priceStep = priceStep;
    }

    public String getProductType() {
        return productType;
    }

    public void setProductType(String productType) {
        this.productType = productType;
    }

    public String getExtraInfo() {
        return extraInfo;
    }

    public void setExtraInfo(String extraInfo) {
        this.extraInfo = extraInfo;
    }

    public String getSellerUsername() {
        return sellerUsername;
    }

    public void setSellerUsername(String sellerUsername) {
        this.sellerUsername = sellerUsername;
    }

    public LocalDateTime getStartTime() {
        return startTime;
    }
    public void setStartTime(LocalDateTime startTime) {
        this.startTime = startTime;
    }

    public LocalDateTime getEndTime() {
        return endTime;
    }
    public void setEndTime(LocalDateTime endTime) {
        this.endTime = endTime;
    }

    /**
     * Hàm toString hỗ trợ việc ghi Log ở cả phía Client và Server khi debug.
     */
    @Override
    public String toString() {
        return "CreateAuctionRequest{" +
                "productName='" + productName + '\'' +
                ", productType='" + productType + '\'' +
                ", startingPrice=" + startingPrice +
                ", priceStep=" + priceStep +
                '}';
    }
}