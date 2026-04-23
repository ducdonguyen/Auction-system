package com.auction.shared.models;

/**
 * Record để hiển thị thông tin phiên đấu giá trong TableView.
 */
public record AuctionRow(
        String auctionId,
        String itemName,
        String sellerName,
        String currentPrice,
        String stepPrice,
        String status,
        String summary,
        String highestBidder
) { }

