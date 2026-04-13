package com.auction.server.core;

import com.auction.server.repository.AuctionRepository;
import com.auction.shared.models.Auction;
import com.auction.shared.models.AuctionStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

@Component
public class AuctionScheduler {

    @Autowired
    private AuctionRepository repository;

    @Autowired
    private AuctionService auctionService;

    /**
     * Tự động quét và cập nhật trạng thái các phiên đấu giá.
     * Chạy mỗi 10 giây một lần.
     */
    @Scheduled(fixedRate = 10000)
    public void autoUpdateAuctions() {
        LocalDateTime now = LocalDateTime.now();

        // 1. Tự động MỞ các phiên đã đến giờ bắt đầu (OPEN -> RUNNING)
        List<Auction> toStart = repository.findByStatusAndStartTimeBefore(AuctionStatus.OPEN, now);
        if (!toStart.isEmpty()) {
            System.out.println("[SCHEDULER] Đang kích hoạt " + toStart.size() + " phiên đấu giá mới...");
            toStart.forEach(auction -> {
                auctionService.updateAuctionStatus(auction, AuctionStatus.RUNNING);
            });
        }

        // 2. Tự động ĐÓNG các phiên đã hết thời gian (RUNNING -> FINISHED)
        List<Auction> toFinish = repository.findByStatusAndEndTimeBefore(AuctionStatus.RUNNING, now);
        if (!toFinish.isEmpty()) {
            System.out.println("[SCHEDULER] Đang kết thúc " + toFinish.size() + " phiên đấu giá đã hết giờ...");
            toFinish.forEach(auction -> {
                auctionService.updateAuctionStatus(auction, AuctionStatus.FINISHED);
            });
        }
    }
}
