package com.auction.shared.models;

import java.io.Serializable;

public class JoinRoomRequest implements Serializable {
    private static final long serialVersionUID = 1L;
    private String auctionId;

    public JoinRoomRequest(String auctionId) {
        this.auctionId = auctionId;
    }

    public String getAuctionId() {
        return auctionId;
    }
}
