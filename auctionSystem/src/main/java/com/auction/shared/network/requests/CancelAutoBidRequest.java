package com.auction.shared.network.requests;

import java.io.Serializable;

/**
 * Gói tin yêu cầu hủy bỏ Đấu giá tự động (Auto-Bidding) trong một phòng cụ thể.
 */
public class CancelAutoBidRequest implements Serializable {
    private static final long serialVersionUID = 1L;

    private String auctionId;

    public CancelAutoBidRequest(String auctionId) {
        this.auctionId = auctionId;
    }

    public String getAuctionId() {
        return auctionId;
    }

    public void setAuctionId(String auctionId) {
        this.auctionId = auctionId;
    }

    @Override
    public String toString() {
        return "CancelAutoBidRequest{" +
                "auctionId='" + auctionId + '\'' +
                '}';
    }
}
