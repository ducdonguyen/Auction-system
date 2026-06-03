// File: AuctionServiceAntiSnipeTest.java
// Location: auctionSystem/src/test/java/com/auction/server/core/AuctionServiceAntiSnipeTest.java

package com.auction.server.core;

import com.auction.server.concurrency.AuctionLockManager;
import com.auction.server.repository.AuctionRepository;
import com.auction.server.service.AuthService;
import com.auction.shared.models.auction.Auction;
import com.auction.shared.models.auction.AuctionStatus;
import com.auction.shared.models.auth.Seller;
import com.auction.shared.models.item.Electronics;
import com.auction.shared.models.item.Item;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.time.Duration;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class AuctionServiceAntiSnipeTest {

    private AuctionService auctionService;
    private AuctionLockManager lockManager;
    private AuctionRepository auctionRepository;
    private AuthService authService;

    @BeforeEach
    void setUp() throws Exception {
        lockManager = spy(new AuctionLockManager());
        auctionRepository = mock(AuctionRepository.class);
        authService = mock(AuthService.class);

        auctionService = new AuctionService(lockManager, auctionRepository);

        Field authField = AuctionService.class.getDeclaredField("authService");
        authField.setAccessible(true);
        authField.set(auctionService, authService);

        when(authService.getBalance(anyString())).thenReturn(1000000000.0);
        when(authService.freezeBalance(anyString(), anyDouble())).thenReturn(true);
        doNothing().when(authService).refundBalance(anyString(), anyDouble());
    }

    @Test
    @DisplayName("✓ ANTI-SNIPE: Khi còn 20s → phải gia hạn 60s")
    void testAntiSnipeExtensionWhen20SecondsLeft() {
        // Setup: Tạo phiên có endTime chỉ còn 20s nữa
        Item item = new Electronics("Laptop", "Test", 1000.0, 12);
        Seller seller = new Seller("seller", "pass");
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime end = now.plusSeconds(20);  // ← QUAN TRỌNG: Chỉ còn 20s

        Auction auction = new Auction("AUC-TEST-1", item, seller, 1000.0, 100.0,
                now.minusHours(1), end);
        auction.setStatus(AuctionStatus.RUNNING);

        when(auctionRepository.findById("AUC-TEST-1")).thenReturn(auction);
        when(auctionRepository.save(any(Auction.class))).thenAnswer(i -> i.getArgument(0));

        // Act: Đặt giá khi còn 20s
        boolean result = auctionService.placeBid("AUC-TEST-1", "bidder1", 1100.0);

        // Assert
        assertTrue(result, "Bid should succeed");

        LocalDateTime newEnd = auction.getEndTime();
        assertNotNull(newEnd, "End time should not be null after bid");

        long extensionDuration = Duration.between(end, newEnd).getSeconds();
        assertEquals(60, extensionDuration,
                "Anti-snipe should extend by 60 seconds when < 30s left");

        assertTrue(newEnd.isAfter(end),
                "New end time should be after original end time");

        System.out.println("✓ ANTI-SNIPE TEST PASSED!");
        System.out.println("  Original end: " + end);
        System.out.println("  New end: " + newEnd);
        System.out.println("  Extension: " + extensionDuration + "s");
    }

    @Test
    @DisplayName("✓ ANTI-SNIPE: Khi còn 5s → phải gia hạn 60s")
    void testAntiSnipeExtensionWhen5SecondsLeft() {
        Item item = new Electronics("Laptop", "Test", 1000.0, 12);
        Seller seller = new Seller("seller", "pass");
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime end = now.plusSeconds(5);  // ← Chỉ còn 5s

        Auction auction = new Auction("AUC-TEST-2", item, seller, 1000.0, 100.0,
                now.minusHours(1), end);
        auction.setStatus(AuctionStatus.RUNNING);

        when(auctionRepository.findById("AUC-TEST-2")).thenReturn(auction);
        when(auctionRepository.save(any(Auction.class))).thenAnswer(i -> i.getArgument(0));

        boolean result = auctionService.placeBid("AUC-TEST-2", "bidder1", 1100.0);

        assertTrue(result);
        assertEquals(60, Duration.between(end, auction.getEndTime()).getSeconds());
        System.out.println("✓ Khi còn 5s: Đã gia hạn 60s");
    }

    @Test
    @DisplayName("✗ NO ANTI-SNIPE: Khi còn 40s → KHÔNG gia hạn")
    void testNoAntiSnipeWhen40SecondsLeft() {
        Item item = new Electronics("Laptop", "Test", 1000.0, 12);
        Seller seller = new Seller("seller", "pass");
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime end = now.plusSeconds(40);  // ← Còn 40s > 30s threshold

        Auction auction = new Auction("AUC-TEST-3", item, seller, 1000.0, 100.0,
                now.minusHours(1), end);
        auction.setStatus(AuctionStatus.RUNNING);

        when(auctionRepository.findById("AUC-TEST-3")).thenReturn(auction);
        when(auctionRepository.save(any(Auction.class))).thenAnswer(i -> i.getArgument(0));

        boolean result = auctionService.placeBid("AUC-TEST-3", "bidder1", 1100.0);

        assertTrue(result);
        assertEquals(end, auction.getEndTime(),
                "End time should NOT change when still > 30s");
        System.out.println("✓ Khi còn 40s: Không gia hạn (đúng)");
    }

    @Test
    @DisplayName("✓ ANTI-SNIPE: Multiple extensions")
    void testMultipleAntiSnipeExtensions() {
        Item item = new Electronics("Laptop", "Test", 1000.0, 12);
        Seller seller = new Seller("seller", "pass");
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime end = now.plusSeconds(20);

        Auction auction = new Auction("AUC-TEST-4", item, seller, 1000.0, 100.0,
                now.minusHours(1), end);
        auction.setStatus(AuctionStatus.RUNNING);

        when(auctionRepository.findById("AUC-TEST-4")).thenReturn(auction);
        when(auctionRepository.save(any(Auction.class))).thenAnswer(i -> i.getArgument(0));

        // Bid 1
        auctionService.placeBid("AUC-TEST-4", "bidder1", 1100.0);
        LocalDateTime end1 = auction.getEndTime();

        // Bid 2 (người khác bid lại khi gần hết - vẫn < 30s so với end1)
        auctionService.placeBid("AUC-TEST-4", "bidder2", 1200.0);
        LocalDateTime end2 = auction.getEndTime();

        assertTrue(end2.isAfter(end1), "Second anti-snipe should extend further");
        System.out.println("✓ Multiple extensions work correctly");
    }
}