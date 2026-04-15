package com.auction.shared.models;

import java.io.Serializable;
import java.time.LocalDateTime;

public class BidTransaction implements Serializable {
    private String transactionId; // Thêm mã giao dịch
    private Bidder bidder;
    private double bidAmount;
    private LocalDateTime timestamp;

    public BidTransaction(String transactionId, Bidder bidder, double bidAmount, LocalDateTime timestamp) {
        this.transactionId = transactionId;
        this.bidder = bidder;
        this.bidAmount = bidAmount;
        this.timestamp = timestamp;
    }

    // --- Getters ---
    public String getTransactionId() {
        return transactionId;
    }

    public Bidder getBidder() {
        return bidder;
    }

    public double getBidAmount() {
        return bidAmount;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }
}