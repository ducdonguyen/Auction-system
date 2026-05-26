package com.auction.shared.network;

import java.io.Serializable;

public class TopUpRequest implements Serializable {
    private static final long serialVersionUID = 1L;

    private String userId;
    private double amount;

    public TopUpRequest(String userId, double amount) {
        this.userId = userId;
        this.amount = amount;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public double getAmount() {
        return amount;
    }

    public void setAmount(double amount) {
        this.amount = amount;
    }
}
