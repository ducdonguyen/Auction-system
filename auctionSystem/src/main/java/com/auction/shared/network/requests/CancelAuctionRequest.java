package com.auction.shared.network.requests;

import java.io.Serializable;

public record CancelAuctionRequest(String auctionId) implements Serializable {
}
