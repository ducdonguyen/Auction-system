package com.auction.shared.network;

import java.io.Serializable;

public class BidRequest implements Serializable {
    private static final long serialVersionUID = 1L;
    private String auctionId;
    private String bidderName;
    private double amount;

    public BidRequest(String auctionId, String bidderName, double amount) {
        this.auctionId = auctionId;
        this.bidderName = bidderName;
        this.amount = amount;
    }

    public String getAuctionId() {
        return auctionId;
    }

    public String getBidderName() {
        return bidderName;
    }

    public double getAmount() {
        return amount;
    }
}
