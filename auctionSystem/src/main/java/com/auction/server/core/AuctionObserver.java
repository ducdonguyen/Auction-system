package com.auction.server.core;

import com.auction.shared.models.AuctionStatus;
import com.auction.shared.models.BidTransaction;

public interface AuctionObserver {
    // Hàm này sẽ được gọi mỗi khi có một mức giá mới được cập nhật thành công
    void updateNewBid(String auctionId, BidTransaction newBid);

    // Hàm này sẽ được gọi mỗi khi trạng thái của phiên đấu giá thay đổi (MỞ, ĐÓNG, KẾT THÚC...)
    void updateStatus(String auctionId, AuctionStatus newStatus);
}