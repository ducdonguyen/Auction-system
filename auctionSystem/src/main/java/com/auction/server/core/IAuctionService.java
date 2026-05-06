package com.auction.server.core;

import com.auction.shared.models.Auction;
import com.auction.shared.models.AuctionStatus;

public interface IAuctionService {
    boolean updateAuctionStatus(Auction auction, AuctionStatus nextStatus);
}