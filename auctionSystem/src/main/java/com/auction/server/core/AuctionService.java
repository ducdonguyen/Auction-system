package com.auction.server.core;

import com.auction.server.concurrency.AuctionLockManager;
import com.auction.server.repository.AuctionRepository;
import com.auction.shared.exceptions.AuctionClosedException;
import com.auction.shared.exceptions.InvalidBidException;
import com.auction.shared.models.Auction;
import com.auction.shared.models.AuctionStatus;
import com.auction.shared.models.BidTransaction;
import com.auction.shared.models.Bidder;
import com.auction.shared.models.Item;
import com.auction.shared.models.Seller;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Lớp dịch vụ điều phối các hoạt động của phiên đấu giá.
 * Đây là nơi tập trung logic nghiệp vụ và xử lý tranh chấp đa luồng.
 * Sử dụng AuctionLockManager để quản lý locking an toàn cho từng phiên.
 */

public class AuctionService {

    private AuctionLockManager lockManager;
    private AuctionRepository auctionRepository;

    public AuctionService() {
    }


    public AuctionService(AuctionLockManager lockManager, AuctionRepository auctionRepository) {
        this.lockManager = lockManager;
        this.auctionRepository = auctionRepository;
    }

    public Auction createAuction(Item item, Seller seller, double startingPrice, double stepPrice,
                                 LocalDateTime startTime, LocalDateTime endTime) {
        String auctionId = "AUC-" + UUID.randomUUID().toString().substring(0, 8);

        Auction newAuction = new Auction(auctionId, item, seller, startingPrice, stepPrice, startTime, endTime);

        newAuction.setStatus(AuctionStatus.OPEN);

        System.out.println("[INFO] Đã tạo phiên đấu giá mới: " + auctionId + " cho mặt hàng " + item.getName());
        return auctionRepository.save(newAuction);
    }

    public boolean placeBid(String auctionId, String bidderUsername, double bidAmount) {
        try {
            Auction auction = auctionRepository.findById(auctionId);
            if (auction == null) {
                System.err.println("[FAILED] Auction không tồn tại: " + auctionId);
                return false;
            }

            Bidder bidder = new Bidder(bidderUsername, "", 0); // Simplified for now

            // Ensure only one person can bid at a time for this auction
            lockManager.lockAndRun(auction.getAuctionId(), () -> performPlaceBid(auction, bidder, bidAmount));

            auctionRepository.save(auction);

            // Notify observers via AuctionManager
            BidTransaction lastBid = auction.getBidHistory().get(auction.getBidHistory().size() - 1);
            AuctionManager.getInstance().notifyObservers(auctionId, lastBid);
            return true;
        } catch (AuctionClosedException | InvalidBidException e) {
            System.err.println("[FAILED] Bid rejected: " + e.getMessage());
            throw new RuntimeException(e.getMessage());
        } catch (Exception e) {
            System.err.println("[ERROR] Unknown error during bidding: " + e.getMessage());
            throw new RuntimeException("Lỗi đặt giá: " + e.getMessage());
        }
    }

    public boolean placeBid(Auction auction, Bidder bidder, double bidAmount) {
        try {
            // Sử dụng lock manager để đảm bảo chỉ một người có thể đặt giá cho phiên này tại một thời điểm
            lockManager.lockAndRun(auction.getAuctionId(), () -> performPlaceBid(auction, bidder, bidAmount));

            auctionRepository.save(auction);
            return true;
        } catch (AuctionClosedException | InvalidBidException e) {
            System.err.println("[FAILED] " + e.getMessage());
            return false;
        } catch (Exception e) {
            System.err.println("[ERROR] Lỗi không xác định khi đặt giá: " + e.getMessage());
            return false;
        }
    }

    private void performPlaceBid(Auction auction, Bidder bidder, double bidAmount) {
        auction.validateBid(bidAmount);

        // Nếu vượt qua validateBid (không ném exception), tiến hành cập nhật trạng thái
        BidTransaction transaction = new BidTransaction(
                "TX-" + System.currentTimeMillis(), // Tạo mã giao dịch tạm thời dựa trên thời gian
                bidder,
                bidAmount,
                LocalDateTime.now()
        );

        auction.updateAuctionState(bidder, bidAmount, transaction);

        System.out.println("[SUCCESS] " + bidder.getUsername() + " đã đặt giá thành công: " + bidAmount);
    }

    public boolean updateAuctionStatus(Auction auction, AuctionStatus nextStatus) {
        // Kiểm tra xem việc chuyển từ trạng thái hiện tại sang trạng thái mới có hợp lệ không
        if (isValidTransition(auction.getStatus(), nextStatus)) {
            AuctionStatus oldStatus = auction.getStatus();
            auction.setStatus(nextStatus);
            auctionRepository.save(auction);

            System.out.println("[INFO] Auction " + auction.getAuctionId()
                    + ": " + oldStatus + " -> " + nextStatus);

            // Broadcast cập nhật trạng thái tới tất cả các Client đang theo dõi
            AuctionManager.getInstance().notifyStatusUpdate(auction.getAuctionId(), nextStatus);

            return true;
        } else {
            System.err.println("[FAILED] Không thể chuyển trạng thái từ "
                    + auction.getStatus() + " sang " + nextStatus);
            return false;
        }
    }

    /**
     * Kiểm tra tính hợp lệ của máy trạng thái (State Machine)
     */
    private boolean isValidTransition(AuctionStatus current, AuctionStatus next) {
        if (current == next) {
            return true; // Không thay đổi gì
        }

        return switch (current) {
            case OPEN -> (next == AuctionStatus.RUNNING || next == AuctionStatus.CANCELED);
            case RUNNING -> (next == AuctionStatus.FINISHED || next == AuctionStatus.CANCELED);
            case FINISHED -> (next == AuctionStatus.PAID || next == AuctionStatus.CANCELED);
            default -> false; // PAID hoặc CANCELED là trạng thái cuối, không thể chuyển đi tiếp
        };
    }
}