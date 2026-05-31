package com.auction.shared.network.requests;

import java.io.Serializable;

/**
 * Gói tin yêu cầu thiết lập Đấu giá tự động (Auto-Bidding) cho một phòng cụ thể.
 */
public class SetAutoBidRequest implements Serializable {
    private static final long serialVersionUID = 1L;

    private String auctionId;
    private double maxBid;
    private double increment;

    public SetAutoBidRequest(String auctionId, double maxBid, double increment) {
        this.auctionId = auctionId;
        this.maxBid = maxBid;
        this.increment = increment;
    }

    public String getAuctionId() {
        return auctionId;
    }

    public void setAuctionId(String auctionId) {
        this.auctionId = auctionId;
    }

    public double getMaxBid() {
        return maxBid;
    }

    public void setMaxBid(double maxBid) {
        this.maxBid = maxBid;
    }

    public double getIncrement() {
        return increment;
    }

    public void setIncrement(double increment) {
        this.increment = increment;
    }

    @Override
    public String toString() {
        return "SetAutoBidRequest{" +
                "auctionId='" + auctionId + '\'' +
                ", maxBid=" + maxBid +
                ", increment=" + increment +
                '}';
    }
}