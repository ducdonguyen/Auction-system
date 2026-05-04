package com.auction.shared.models;
import com.auction.shared.exceptions.*;
import org.slf4j.*;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.*;
public class Auction implements Serializable {
    private static final Logger logger = LoggerFactory.getLogger(Auction.class);
    private static final long serialVersionUID = 1L;
    private String auctionId;
    private Item item;
    private Seller seller;
    private LocalDateTime startTime, endTime;
    private double currentPrice, stepPrice;
    private Bidder highestBidder;
    private List<BidTransaction> bidHistory = new ArrayList<>();
    private AuctionStatus status = AuctionStatus.OPEN;
    public Auction(String auctionId, Item item, Seller seller, double startingPrice, double stepPrice, LocalDateTime startTime, LocalDateTime endTime) {
        this.auctionId = auctionId; this.item = item; this.seller = seller; this.currentPrice = startingPrice; this.stepPrice = stepPrice;
        this.startTime = startTime; this.endTime = endTime;
    }
    public boolean validateBid(double amount) throws AuctionClosedException, InvalidBidException {
        if (status != AuctionStatus.RUNNING) throw new AuctionClosedException("Auction " + auctionId + " not running.");
        if (amount <= 0) throw new InvalidBidException("Bid must be > 0.");
        if (amount < (currentPrice + stepPrice)) throw new InvalidBidException("Bid " + amount + " too low. Min: " + (currentPrice + stepPrice));
        return true;
    }
    public void updateAuctionState(Bidder bidder, double amount, BidTransaction transaction) {
        this.currentPrice = amount; this.highestBidder = bidder;
        if (transaction != null) this.bidHistory.add(transaction);
        logger.debug("[LOG] Auction {} updated: {} by {}", auctionId, amount, bidder.getUsername());
    }
    public double getCurrentPrice() { return currentPrice; }
    public double getStepPrice() { return stepPrice; }
    public Bidder getHighestBidder() { return highestBidder; }
    public List<BidTransaction> getBidHistory() { return Collections.unmodifiableList(bidHistory); }
    public String getAuctionId() { return auctionId; }
    public void setAuctionId(String auctionId) { this.auctionId = auctionId; }
    public Item getItem() { return item; }
    public void setItem(Item item) { this.item = item; }
    public Seller getSeller() { return seller; }
    public void setSeller(Seller seller) { this.seller = seller; }
    public AuctionStatus getStatus() { return status; }
    public void setStatus(AuctionStatus status) { this.status = status; }
    public void setStartTime(LocalDateTime startTime) { this.startTime = startTime; }
    public LocalDateTime getStartTime() { return startTime; }
    public void setEndTime(LocalDateTime endTime) { this.endTime = endTime; }
    public LocalDateTime getEndTime() { return endTime; }
}