package com.auction.server;

import com.auction.shared.Auction;
import java.util.concurrent.ConcurrentHashMap;

import com.auction.shared.Auction;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;

public class AuctionManager {
    private static AuctionManager instance;
    private Map<String, Auction> activeAuctions;

    private AuctionManager() {
        activeAuctions = new ConcurrentHashMap<>();
    }

    public static synchronized AuctionManager getInstance() {
        if (instance == null) {
            instance = new AuctionManager();
        }
        return instance;
    }

    public void addAuction(Auction auction) {
        if (auction == null) {
            throw new IllegalArgumentException("Auction không thể null");
        }

        if (auction.getAuctionId() == null || auction.getAuctionId().trim().isEmpty()) {
            throw new IllegalArgumentException("Auction ID không thể null hoặc để trống");
        }

        Auction existingAuction = activeAuctions.putIfAbsent(auction.getAuctionId(), auction);
        if (existingAuction != null) {
            throw new IllegalArgumentException("Auction với ID " + auction.getAuctionId() + " đã tồn tại");
        }
    }
}