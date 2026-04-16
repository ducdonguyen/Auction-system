package com.auction.demo.client.service;
import com.auction.shared.models.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
public class AuctionDataStore {
    private static final List<Auction> auctions = new ArrayList<>();
    static {
        Seller s = new Seller("admin_seller", "123");
        Item i1 = ItemFactory.createItem("ELECTRONICS", "Laptop Dell XPS 15", "Laptop cao cấp 2024", 25000000, "12");
        Auction a1 = new Auction("AUC001", i1, s, 25000000, 500000, LocalDateTime.now().minusHours(1), LocalDateTime.now().plusHours(5));
        a1.setStatus(AuctionStatus.RUNNING);
        Item i2 = ItemFactory.createItem("VEHICLE", "VinFast VF8", "Xe điện thông minh", 1000000000, "29A-123.45");
        Auction a2 = new Auction("AUC002", i2, s, 1000000000, 10000000, LocalDateTime.now().plusDays(1), LocalDateTime.now().plusDays(2));
        auctions.add(a1); auctions.add(a2);
    }
    public static List<Auction> getAuctions() { return auctions; }
    public static Optional<Auction> findById(String id) { return auctions.stream().filter(a -> a.getAuctionId().equals(id)).findFirst(); }
}
