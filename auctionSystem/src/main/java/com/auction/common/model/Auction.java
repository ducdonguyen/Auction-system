package com.auction.common.model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class Auction implements Serializable {
    private String auctionId;
    private Item item;
    private Seller seller;
    private List<BidTransaction> bidHistory;
    private String status; // OPEN, RUNNING, FINISHED

    public Auction(String auctionId, Item item, Seller seller) {
        this.auctionId = auctionId;
        this.item = item;
        this.seller = seller;
        this.bidHistory = new ArrayList<>();
        this.status = "OPEN";
    }

    public String getAuctionId() {
        return auctionId;
    }

    public void setAuctionId(String auctionId) {
        this.auctionId = auctionId;
    }

    public Item getItem() {
        return item;
    }

    public void setItem(Item item) {
        this.item = item;
    }

    public Seller getSeller() {
        return seller;
    }

    public void setSeller(Seller seller) {
        this.seller = seller;
    }

    public List<BidTransaction> getBidHistory() {
        return bidHistory;
    }

    public void setBidHistory(List<BidTransaction> bidHistory) {
        this.bidHistory = bidHistory;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public void addBid(BidTransaction bid) {
        this.bidHistory.add(bid);
    }
}