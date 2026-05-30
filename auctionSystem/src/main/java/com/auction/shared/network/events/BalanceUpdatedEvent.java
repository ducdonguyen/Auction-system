package com.auction.shared.network.events;

import java.io.Serializable;

/**
 * Gói tin Event dạng Class truyền thống dùng để thông báo cập nhật số dư qua Socket.
 */
public class BalanceUpdatedEvent implements Serializable {
    private static final long serialVersionUID = 1L;

    private double newBalance;
    private double amountChanged;
    private String reason;

    public BalanceUpdatedEvent() {
    }

    // Constructor 3 tham số toàn diện cho hệ thống nhảy số dư chính xác
    public BalanceUpdatedEvent(double newBalance, double amountChanged, String reason) {
        this.newBalance = newBalance;
        this.amountChanged = amountChanged;
        this.reason = reason;
    }

    // Constructor 2 tham số giúp tương thích ngược an toàn
    public BalanceUpdatedEvent(double newBalance, String reason) {
        this.newBalance = newBalance;
        this.amountChanged = 0.0;
        this.reason = reason;
    }

    // --- CÁC HÀM GETTER VÀ SETTER CHUẨN CLASS ---

    public double getNewBalance() {
        return newBalance;
    }

    public void setNewBalance(double newBalance) {
        this.newBalance = newBalance;
    }

    public double getAmountChanged() {
        return amountChanged;
    }

    public void setAmountChanged(double amountChanged) {
        this.amountChanged = amountChanged;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }
}