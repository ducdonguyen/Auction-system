package com.auction.server.repository;

import com.auction.shared.models.Auction;
import com.auction.shared.models.AuctionStatus;

import java.time.LocalDateTime;
import java.util.List;

public interface IAuctionRepository {
    Auction findById(String auctionId);

    Auction save(Auction auction);

    List<Auction> findByStatusAndStartTimeBefore(AuctionStatus status, LocalDateTime time);

    List<Auction> findByStatusAndEndTimeBefore(AuctionStatus status, LocalDateTime time);
}