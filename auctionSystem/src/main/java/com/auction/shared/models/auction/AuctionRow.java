package com.auction.shared.models.auction;

/**
 * Record để hiển thị thông tin phiên đấu giá trong TableView.
 */
public record AuctionRow(
        String auctionId,
        String itemName,
        String sellerName,
        String highestBidder,
        String currentPrice,
        String stepPrice,
        String status,
        String summary
) {
}

