package com.auction.shared.network;

import java.io.Serializable;

public record CancelAuctionRequest(String auctionId) implements Serializable {
}
