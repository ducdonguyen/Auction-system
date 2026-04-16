package com.auction.server.core;

import com.auction.shared.models.BidTransaction;

public interface AuctionObserver {
    // Hàm này sẽ được gọi mỗi khi có một mức giá mới được cập nhật thành công
    void updateNewBid(String auctionId, BidTransaction newBid);
}