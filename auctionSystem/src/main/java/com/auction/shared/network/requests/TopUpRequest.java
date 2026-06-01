package com.auction.shared.network.requests;

import java.io.Serializable;

public class TopUpRequest implements Serializable {
    private static final long serialVersionUID = 1L;

    private String userName;
    private double amount;

    public TopUpRequest(String userName, double amount) {
        this.userName = userName;
        this.amount = amount;
    }

    public String getUserId() {
        return userName;
    }

    public void setUserId(String userId) {
        this.userName = userName;
    }

    public double getAmount() {
        return amount;
    }

    public void setAmount(double amount) {
        this.amount = amount;
    }
}
