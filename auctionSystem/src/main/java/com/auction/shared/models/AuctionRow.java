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
) {
    public String getAuctionId() {
        return auctionId;
    }

    public String getItemName() {
        return itemName;
    }

    public String getSellerName() {
        return sellerName;
    }

    public String getCurrentPrice() {
        return currentPrice;
    }

    public String getStepPrice() {
        return stepPrice;
    }

    public String getStatus() {
        return status;
    }

    public String getSummary() {
        return summary;
    }

    public String getHighestBidder() {
        return highestBidder;
    }
}
