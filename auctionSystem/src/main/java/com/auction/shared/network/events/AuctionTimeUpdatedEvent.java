package com.auction.shared.network.events;

import java.io.Serializable;

public class AuctionTimeUpdatedEvent implements Serializable {
    private static final long serialVersionUID = 1L;
    private final String auctionId;
    private final long newEndMillis;

    public AuctionTimeUpdatedEvent(String auctionId, long newEndMillis) {
        this.auctionId = auctionId;
        this.newEndMillis = newEndMillis;
    }

    public String getAuctionId() {
        return auctionId;
    }

    public long getNewEndMillis() {
        return newEndMillis;
    }
}