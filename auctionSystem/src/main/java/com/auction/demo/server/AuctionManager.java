package com.auction.demo.server;

import com.auction.demo.common.model.Auction;
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
            // Chỉ lock khi bắt đầu khởi tạo lần đầu tiên
            synchronized (AuctionManager.class) {
                // Kiểm tra lần 2: Đề phòng trường hợp luồng khác đã kịp khởi tạo instance
                // trong lúc luồng này đang chờ lấy lock
                if (instance == null) {
                    instance = new AuctionManager();
                }
            }
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