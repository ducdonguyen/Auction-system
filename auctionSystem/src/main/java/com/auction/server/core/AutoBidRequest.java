package com.auction.server.core;

import java.time.LocalDateTime;

/**
 * Đại diện cho một yêu cầu Auto-bid của người dùng.
 */
public class AutoBidRequest implements Comparable<AutoBidRequest> {
    private final String bidderUsername;
    private final double maxBid;
    private final double increment;
    private final LocalDateTime setupTime;

    public AutoBidRequest(String bidderUsername, double maxBid, double increment) {
        this.bidderUsername = bidderUsername;
        this.maxBid = maxBid;
        this.increment = increment;
        this.setupTime = LocalDateTime.now();
    }

    public String getBidderUsername() {
        return bidderUsername;
    }

    public double getMaxBid() {
        return maxBid;
    }

    public double getIncrement() {
        return increment;
    }

    public LocalDateTime getSetupTime() {
        return setupTime;
    }

    @Override
    public int compareTo(AutoBidRequest o) {
        // Sắp xếp maxBid giảm dần (người cài giá trần cao nhất lên trước)
        int compareMaxBid = Double.compare(o.maxBid, this.maxBid);
        if (compareMaxBid != 0) {
            return compareMaxBid;
        }
        // Nếu cùng giá trần, ưu tiên ai cài trước (setupTime tăng dần)
        return this.setupTime.compareTo(o.setupTime);
    }
}
