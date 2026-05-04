package com.auction.shared.models;

import java.io.Serializable;
import java.time.LocalDateTime;

public record BidTransaction(String transactionId, Bidder bidder, double bidAmount,
                             LocalDateTime timestamp) implements Serializable {
    private static final long serialVersionUID = 1L;
}