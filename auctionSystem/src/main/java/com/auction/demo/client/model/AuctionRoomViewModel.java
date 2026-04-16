package com.auction.demo.client.model;
import java.util.List;
public record AuctionRoomViewModel(String auctionId, String itemName, String sellerName, String status, String currentPrice, String stepPrice, String minimumBid, String highestBidder, String description, String schedule, List<String> bidHistory) {}
