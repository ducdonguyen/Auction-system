package com.auction.shared.network.requests;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * Lớp Đóng gói dữ liệu (DTO) gửi yêu cầu tạo phiên đấu giá mới từ Client lên Server.
 */
public class CreateAuctionRequest implements Serializable {
    private static final long serialVersionUID = 1L;

    private String productName;
    private String description;
    private double startingPrice;
    private double priceStep;
    private String productType;
    private String extraInfo;     // VD: Nếu là Electronics thì là thời gian bảo hành, Art thì là tên tác giả, v.v
    private String sellerUsername;

    // Client có thể gửi startTime/endTime trực tiếp (hiện tại) OR gửi duration để server tính
    private LocalDateTime startTime;
    private LocalDateTime endTime;

    // MỚI: thời lượng (nullable). Nếu khác null => server sẽ tính start/end dựa trên server time
    private Long durationValue; // số (ví dụ: 3)
    private String durationUnit; // "DAYS", "HOURS", "MINUTES" hoặc "Ngày","Giờ","Phút" tùy bạn

    public CreateAuctionRequest() {
    }

    /**
     * Constructor cũ (vẫn còn dùng được) - giữ tương thích
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

    /**
     * Constructor MỚI: gửi duration thay vì endTime
     */
    public CreateAuctionRequest(String productName, String description, double startingPrice, double priceStep,
                                String productType, String extraInfo, String sellerUsername,
                                Long durationValue, String durationUnit) {
        this.productName = productName;
        this.description = description;
        this.startingPrice = startingPrice;
        this.priceStep = priceStep;
        this.productType = productType;
        this.extraInfo = extraInfo;
        this.sellerUsername = sellerUsername;
        this.durationValue = durationValue;
        this.durationUnit = durationUnit;
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

    public Long getDurationValue() {
        return durationValue;
    }

    public void setDurationValue(Long durationValue) {
        this.durationValue = durationValue;
    }

    public String getDurationUnit() {
        return durationUnit;
    }

    public void setDurationUnit(String durationUnit) {
        this.durationUnit = durationUnit;
    }

    @Override
    public String toString() {
        return "CreateAuctionRequest{" +
                "productName='" + productName + '\'' +
                ", productType='" + productType + '\'' +
                ", startingPrice=" + startingPrice +
                ", priceStep=" + priceStep +
                ", durationValue=" + durationValue +
                ", durationUnit='" + durationUnit + '\'' +
                '}';
    }
}