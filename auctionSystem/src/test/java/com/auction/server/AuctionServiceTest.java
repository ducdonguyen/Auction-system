package com.auction.server;

import com.auction.server.concurrency.AuctionLockManager;
import com.auction.server.core.AuctionService;
import com.auction.server.repository.AuctionRepository;
import com.auction.shared.models.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

public class AuctionServiceTest {

    private AuctionService auctionService;
    private Item laptop;
    private Seller seller;

    @BeforeEach
    public void setUp() {
        AuctionLockManager lockManager = new AuctionLockManager();
        AuctionRepository fakeRepository = new AuctionRepository() {
            @Override
            public Auction save(Auction auction) {
                return auction;
            }
        };
        auctionService = new AuctionService(lockManager, fakeRepository);

        laptop = new Item("IT01", "Macbook Air", 1000.0) {
            @Override
            public String getItemType() { return "Electronics"; }
            @Override
            public String getExtraInfo() { return "RAM 16GB, SSD 512GB"; }
        };
        seller = new Seller("S01", "Cửa hàng điện máy");
    }

    @Test
    @DisplayName("Kiểm thử tạo phiên đấu giá")
    public void testCreateAuction() {
        Auction myAuction = createTestAuction(1000.0, 50.0);
        assertNotNull(myAuction, "Phiên đấu giá không được null");
        assertNotNull(myAuction.getAuctionId(), "ID phiên đấu giá không được null");
    }

    @Test
    @DisplayName("Kiểm thử nghiệp vụ đặt giá")
    public void testBiddingLogic() {
        Auction myAuction = createTestAuction(1000.0, 50.0);
        myAuction.setStatus(AuctionStatus.RUNNING);

        Bidder bidderA = new Bidder("User_A", "passA", 2000.0);
        Bidder bidderB = new Bidder("User_B", "passB", 2000.0);

        // TEST 1: Đặt giá thấp hơn mức tối thiểu (1000 + 50 = 1050)
        boolean res1 = auctionService.placeBid(myAuction, bidderA, 1020.0);
        assertFalse(res1, "Hệ thống phải chặn mức giá thấp hơn giá hiện tại + bước giá");

        // TEST 2: Đặt giá hợp lệ
        boolean res2 = auctionService.placeBid(myAuction, bidderA, 1100.0);
        assertTrue(res2, "Đặt giá hợp lệ phải thành công");
        assertEquals(1100.0, myAuction.getCurrentPrice(), "Giá hiện tại phải được cập nhật");

        // TEST 3: Đặt giá khi phiên đã kết thúc
        myAuction.setStatus(AuctionStatus.FINISHED);
        boolean res3 = auctionService.placeBid(myAuction, bidderB, 1500.0);
        assertFalse(res3, "Hệ thống phải chặn đặt giá khi phiên đã kết thúc");
    }

    @Test
    @DisplayName("Kiểm thử đa luồng (Concurrency)")
    public void testConcurrency() throws InterruptedException {
        Auction syncAuction = createTestAuction(2000.0, 100.0);
        syncAuction.setStatus(AuctionStatus.RUNNING);

        int numberOfBidders = 5;
        double bidAmount = 2100.0;
        ExecutorService executor = Executors.newFixedThreadPool(numberOfBidders);

        for (int i = 0; i < numberOfBidders; i++) {
            final int id = i;
            executor.submit(() -> {
                Bidder competitor = new Bidder("Competitor_" + id, "pass", 5000.0);
                auctionService.placeBid(syncAuction, competitor, bidAmount);
            });
        }

        executor.shutdown();
        assertTrue(executor.awaitTermination(10, TimeUnit.SECONDS), "Executor không kết thúc kịp thời");

        // Chỉ có 1 người đặt giá thành công nếu tất cả đặt cùng một mức giá (2100.0)
        // và giá khởi điểm là 2000, bước giá 100 => giá tiếp theo tối thiểu là 2100.
        // Sau khi người đầu tiên đặt 2100, người tiếp theo phải đặt >= 2200.
        assertEquals(1, syncAuction.getBidHistory().size(), "Chỉ một giao dịch thành công khi nhiều người đặt cùng mức giá tối thiểu");
    }

    private Auction createTestAuction(double startingPrice, double stepPrice) {
        return auctionService.createAuction(
                laptop, seller, startingPrice, stepPrice,
                LocalDateTime.now().plusDays(1),
                LocalDateTime.now().plusDays(2)
        );
    }
}