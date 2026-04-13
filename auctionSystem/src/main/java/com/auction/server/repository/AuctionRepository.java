package com.auction.server.repository;

import com.auction.shared.models.Auction;
import com.auction.shared.models.AuctionStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface AuctionRepository extends JpaRepository<Auction, String> {
    
    // Tìm các phiên đang OPEN nhưng đã đến giờ START
    List<Auction> findByStatusAndStartTimeBefore(AuctionStatus status, LocalDateTime now);
    
    // Tìm các phiên đang RUNNING nhưng đã đến giờ END
    List<Auction> findByStatusAndEndTimeBefore(AuctionStatus status, LocalDateTime now);
}
