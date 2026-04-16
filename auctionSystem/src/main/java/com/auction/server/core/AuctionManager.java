package com.auction.server.core;

import com.auction.shared.exceptions.AuthenticationException;
import com.auction.shared.models.Auction;
import com.auction.shared.models.BidTransaction;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

public class AuctionManager {
    private static volatile AuctionManager instance;
    private final Map<String, Auction> activeAuctions;
    private final Map<String, List<AuctionObserver>> observersMap;

    private AuctionManager() {
        activeAuctions = new ConcurrentHashMap<>();
        observersMap = new ConcurrentHashMap<>();
    }

    public static AuctionManager getInstance() {
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

    public void addAuction(Auction auction, String authToken) throws AuthenticationException {
        // Giả lập kiểm tra token để "cấy" AuthenticationException
        if (authToken == null || !authToken.equals("ADMIN_SECRET_TOKEN")) {
            throw new AuthenticationException("Phiên làm việc không hợp lệ hoặc đã hết hạn. Vui lòng đăng nhập lại.");
        }

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

    public void subscribe(String auctionId, AuctionObserver observer) {
        observersMap.computeIfAbsent(auctionId, k -> new CopyOnWriteArrayList<>()).add(observer);
        System.out.println("Có Client vừa đăng ký xem phiên đấu giá ID: " + auctionId);
    }

    public void unsubscribe(String auctionId, AuctionObserver observer) {
        List<AuctionObserver> observers = observersMap.get(auctionId);
        if (observers != null) {
            observers.remove(observer);
            System.out.println("Một Client đã thoát khỏi phiên đấu giá ID: " + auctionId);
        }
    }

    public void notifyObservers(String auctionId, BidTransaction newBid) {
        List<AuctionObserver> observers = observersMap.get(auctionId);
        if (observers != null) {
            for (AuctionObserver observer : observers) {
                observer.updateNewBid(auctionId, newBid);
            }
        }
    }
}
