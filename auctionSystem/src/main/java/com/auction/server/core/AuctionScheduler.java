package com.auction.server.core;

import com.auction.server.repository.AuctionRepository;
import com.auction.shared.models.Auction;
import com.auction.shared.models.AuctionStatus;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Lớp lập lịch tự động cập nhật trạng thái các phiên đấu giá.
 */
public class AuctionScheduler {
  private static final Logger logger = LoggerFactory.getLogger(AuctionScheduler.class);
  private final AuctionRepository repository;
  private final AuctionService auctionService;
  private final ScheduledExecutorService scheduler;

  /**
   * Khởi tạo AuctionScheduler.
   * S.O.L.I.D (DIP): Tiêm (Inject) scheduler từ bên ngoài vào.
   * Giúp class này dễ dàng được kiểm thử (Unit Test) bằng các mock scheduler.
   *
   * @param repository     Repository đấu giá.
   * @param auctionService Dịch vụ đấu giá.
   * @param scheduler      Dịch vụ lập lịch.
   */
  public AuctionScheduler(AuctionRepository repository, AuctionService auctionService,
                          ScheduledExecutorService scheduler) {
    this.repository = repository;
    this.auctionService = auctionService;
    this.scheduler = scheduler;
  }

  /**
   * Bắt đầu lập lịch chạy tự động cập nhật mỗi 10 giây.
   */
  public void startScheduling() {
    // Chạy hàm autoUpdateAuctions() mỗi 10 giây
    scheduler.scheduleAtFixedRate(this::autoUpdateAuctions, 0, 10, TimeUnit.SECONDS);
    logger.info("[SCHEDULER] Hệ thống tự động cập nhật thời gian đã khởi động.");
  }

  private void autoUpdateAuctions() {
    LocalDateTime now = LocalDateTime.now();

    // 1. MỞ phiên
    List<Auction> toStart = repository.findByStatusAndStartTimeBefore(AuctionStatus.OPEN, now);
    if (toStart != null && !toStart.isEmpty()) {
      toStart.forEach(auction -> auctionService.updateAuctionStatus(auction, AuctionStatus.RUNNING));
    }

    // 2. ĐÓNG phiên
    List<Auction> toFinish = repository.findByStatusAndEndTimeBefore(AuctionStatus.RUNNING, now);
    if (toFinish != null && !toFinish.isEmpty()) {
      toFinish.forEach(auction ->
          auctionService.updateAuctionStatus(auction, AuctionStatus.FINISHED));
    }
  }
}