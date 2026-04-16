package com.auction.server;

import com.auction.server.concurrency.AuctionLockManager;
import com.auction.server.core.AuctionService;
import com.auction.server.repository.AuctionRepository;
import com.auction.shared.models.Auction;
import com.auction.shared.models.AuctionStatus;
import com.auction.shared.models.Bidder;
import com.auction.shared.models.Item;
import com.auction.shared.models.Seller;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;


public class AuctionServiceTest {

    @Test
    public void testConcurrency() throws InterruptedException {
        // 1. Khởi tạo dịch vụ cùng với LockManager và Mock Repository
        AuctionLockManager lockManager = new AuctionLockManager();
        AuctionRepository fakeRepository = new AuctionRepository() {
            @Override
            public Auction save(Auction auction) {
                // Giả lập hệ thống lưu Database thành công và trả về chính nó
                return auction;
            }

        };

        AuctionService auctionService = new AuctionService(lockManager, fakeRepository);

        // 2. Kiểm thử việc TẠO PHIÊN (Sử dụng auctionService thay vì service)
        // Lưu ý: Item là abstract nên ta dùng class ẩn danh {}
        Item laptop = new Item("IT01", "Macbook Air", 1000.0) {
        };
        Seller seller = new Seller("S01", "Cửa hàng điện máy");

        // Gọi đúng tên biến auctionService
        Auction myAuction =
                auctionService.createAuction(laptop, seller, 1000.0, 50.0, LocalDateTime.parse("2026-04-13T10:30:00"),
                        LocalDateTime.parse("2026-05-13T10:30:00"));

        System.out.println("========== KIỂM THỬ TẠO PHIÊN ==========");
        if (myAuction != null && myAuction.getAuctionId() != null) {
            System.out.println("=> PASS: Tạo phiên thành công. ID: " + myAuction.getAuctionId());
        }

        // 3. Chuẩn bị dữ liệu cho kiểm thử đặt giá
        // Không khai báo lại 'laptop' và 'seller', chỉ sử dụng lại hoặc gán giá trị mới
        Bidder bidderA = new Bidder("User_A", "passA", 2.0); // Bỏ tham số 2.0 nếu constructor không có
        Bidder bidderB = new Bidder("User_B", "passB", 2.0);

        System.out.println("\n========== BẮT ĐẦU KIỂM THỬ NGHIỆP VỤ ==========");
        // Đảm bảo phiên đấu giá đang chạy
        myAuction.setStatus(AuctionStatus.RUNNING);
        System.out.println("Giá hiện tại: " + myAuction.getCurrentPrice() + ", Bước giá: " + myAuction.getStepPrice());

        // --- TEST 1: Đặt giá thấp hơn mức tối thiểu (1000 + 50 = 1050) ---
        System.out.println("\n[Test 1] Đặt giá 1020.0:");
        boolean res1 = auctionService.placeBid(myAuction, bidderA, 1020.0);
        checkResult(!res1, "Hệ thống đã chặn mức giá thấp (Thành công)");

        // --- TEST 2: Đặt giá hợp lệ ---
        System.out.println("\n[Test 2] Đặt giá 1100.0:");
        boolean res2 = auctionService.placeBid(myAuction, bidderA, 1100.0);
        checkResult(res2 && myAuction.getCurrentPrice() == 1100.0,
                "Đặt giá thành công. Giá mới: " + myAuction.getCurrentPrice());

        // --- TEST 3: Đặt giá khi phiên đã kết thúc ---
        System.out.println("\n[Test 3] Đặt giá khi trạng thái là 'FINISHED':");
        myAuction.setStatus(AuctionStatus.FINISHED);
        boolean res3 = auctionService.placeBid(myAuction, bidderB, 1500.0);
        checkResult(!res3, "Hệ thống đã chặn đặt giá thành công.");

        // --- TEST 4: KIỂM THỬ ĐỒNG THỜI (CONCURRENCY) ---
        System.out.println("\n========== BẮT ĐẦU KIỂM THỬ ĐA LUỒNG ==========");
        // Tạo một phiên đấu giá mới hoàn toàn để test đa luồng
        Auction syncAuction =
                auctionService.createAuction(laptop, seller, 2000.0, 100.0, LocalDateTime.parse("2026-04-13T10:30:00"),
                        LocalDateTime.parse("2026-05-13T10:30:00"));
        syncAuction.setStatus(AuctionStatus.RUNNING);

        System.out.println("Giá khởi điểm: 2000.0. 5 người cùng đặt 2100.0...");

        ExecutorService executor = Executors.newFixedThreadPool(50);
        for (int i = 0; i < 5; i++) {
            final int id = i;
            executor.submit(() -> {
                Bidder competitor = new Bidder("Competitor_" + id, "pass", 2.0);
                auctionService.placeBid(syncAuction, competitor, 2100.0);
            });
        }

        executor.shutdown();
        executor.awaitTermination(10, TimeUnit.SECONDS);

        System.out.println("\n--- KẾT QUẢ CUỐI CÙNG ---");
        int historySize = syncAuction.getBidHistory().size();
        System.out.println("Số giao dịch thành công: " + historySize);

        if (historySize == 1) {
            System.out.println("=> KẾT LUẬN: PASS (Synchronized hoạt động tốt)");
        } else {
            System.out.println("=> KẾT LUẬN: FAIL (Lỗi Race Condition - Có " + historySize + " người cùng thắng)");
        }
    }

    private void checkResult(boolean condition, String message) {
        if (condition) {
            System.out.println("   [PASS] " + message);
        } else {
            System.out.println("   [FAIL] " + message);
        }
    }
}