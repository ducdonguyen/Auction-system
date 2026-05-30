package com.auction.server.core;

import com.auction.shared.models.Auction;
import com.auction.shared.models.Bidder;
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

    public synchronized void registerAutoBid(String auctionId, String bidderUsername, double maxBid) {
        PriorityQueue<AutoBidRequest> queue = auctionAutoBids.computeIfAbsent(auctionId, k -> new PriorityQueue<>());
        queue.removeIf(req -> req.getBidderUsername().equals(bidderUsername));
        queue.offer(new AutoBidRequest(bidderUsername, maxBid));
        logger.info("[AutoBidManager] Đã bật Auto-bid cho user {} tại phòng {} với giá trần {}",
                bidderUsername, auctionId, maxBid);
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

        double nextBidPrice = auction.getCurrentPrice() + auction.getStepPrice();
        Bidder highestBidder = auction.getHighestBidder();
        String highestBidderUsername = (highestBidder != null) ? highestBidder.getUsername() : null;

        List<AutoBidRequest> sortedRequests = new ArrayList<>(queue);
        Collections.sort(sortedRequests);

        for (AutoBidRequest req : sortedRequests) {
            if (req.getBidderUsername().equals(highestBidderUsername)) {
                continue;
            }
            if (nextBidPrice <= req.getMaxBid()) {
                return req;
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
