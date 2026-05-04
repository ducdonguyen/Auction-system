package com.auction.shared.models;

import java.io.Serializable;
import java.time.LocalDateTime;

public class BidTransaction implements Serializable {
    private static final long serialVersionUID = 1L;
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