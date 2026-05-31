package com.auction.server.core;

import com.auction.shared.models.auction.Auction;
import com.auction.shared.models.auth.Bidder;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.concurrent.ConcurrentHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Quản lý danh sách các yêu cầu Auto-bid trong từng phòng đấu giá.
 */
public class AutoBidManager {
    private static final Logger logger = LoggerFactory.getLogger(AutoBidManager.class);
    private final Map<String, PriorityQueue<AutoBidRequest>> auctionAutoBids = new ConcurrentHashMap<>();

    public synchronized void registerAutoBid(String auctionId, String bidderUsername, double maxBid, double increment) {
        PriorityQueue<AutoBidRequest> queue = auctionAutoBids.computeIfAbsent(auctionId, k -> new PriorityQueue<>());
        queue.removeIf(req -> req.getBidderUsername().equals(bidderUsername));

        // Nhồi increment vào Hàng đợi ưu tiên
        queue.offer(new AutoBidRequest(bidderUsername, maxBid, increment));

        logger.info("[AutoBidManager] Đã bật Auto-bid cho user {} tại phòng {}. Giá trần: {}, Bước nhảy: {}",
                bidderUsername, auctionId, maxBid, increment);
    }

    public synchronized void removeAutoBid(String auctionId, String bidderUsername) {
        PriorityQueue<AutoBidRequest> queue = auctionAutoBids.get(auctionId);
        if (queue != null) {
            boolean removed = queue.removeIf(req -> req.getBidderUsername().equals(bidderUsername));
            if (removed) {
                logger.info("[AutoBidManager] Đã tắt Auto-bid cho user {} tại phòng {}", bidderUsername, auctionId);
            }
        }
    }

    public synchronized AutoBidRequest findEligibleAutoBidder(String auctionId, Auction auction) {
        PriorityQueue<AutoBidRequest> queue = auctionAutoBids.get(auctionId);
        if (queue == null || queue.isEmpty()) {
            return null;
        }

        Bidder highestBidder = auction.getHighestBidder();
        String highestBidderUsername = (highestBidder != null) ? highestBidder.getUsername() : null;

        List<AutoBidRequest> sortedRequests = new ArrayList<>(queue);
        Collections.sort(sortedRequests);

        for (AutoBidRequest req : sortedRequests) {
            if (req.getBidderUsername().equals(highestBidderUsername)) {
                continue; // Bỏ qua nếu người này đang dẫn đầu rồi
            }

            // Tính giá thầu tiếp theo dựa trên increment của CÁ NHÂN người dùng này.
            // Math.max để phòng hờ trường hợp Client hack gửi increment < bước giá tối thiểu của phòng
            double actualIncrement = Math.max(req.getIncrement(), auction.getStepPrice());
            double nextBidPrice = auction.getCurrentPrice() + actualIncrement;

            if (nextBidPrice <= req.getMaxBid()) {
                return req; // Tìn thấy người đủ điều kiện
            }
        }
        return null;
    }

    public synchronized List<AutoBidRequest> getAutoBids(String auctionId) {
        PriorityQueue<AutoBidRequest> queue = auctionAutoBids.get(auctionId);
        if (queue == null) {
            return Collections.emptyList();
        }
        List<AutoBidRequest> list = new ArrayList<>(queue);
        Collections.sort(list);
        return list;
    }
}
