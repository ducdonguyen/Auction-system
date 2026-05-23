package com.auction.server.core;

import com.auction.server.concurrency.AuctionLockManager;
import com.auction.server.repository.AuctionRepository;
import com.auction.shared.models.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class AuctionServiceTest {

    private AuctionService auctionService;
    private AuctionLockManager lockManager;
    private AuctionRepository auctionRepository;

    @BeforeEach
    void setUp() {
        lockManager = spy(new AuctionLockManager());
        auctionRepository = mock(AuctionRepository.class);
        auctionService = new AuctionService(lockManager, auctionRepository);
    }

    @Test
    @DisplayName("Kiểm thử tạo phiên đấu giá")
    void testCreateAuction() {
        Item item = new Electronics("Laptop", "MacBook", 1000.0, 12);
        Seller seller = new Seller("s1", "p1");
        LocalDateTime start = LocalDateTime.now();
        LocalDateTime end = start.plusHours(2);

        when(auctionRepository.save(any(Auction.class))).thenAnswer(i -> i.getArgument(0));

        Auction created = auctionService.createAuction(item, seller, 1000.0, 100.0, start, end);

        assertNotNull(created);
        assertEquals(item, created.getItem());
        assertEquals(seller, created.getSeller());
        assertEquals(AuctionStatus.PENDING, created.getStatus());
        verify(auctionRepository).save(created);
    }

    @Test
    @DisplayName("Kiểm thử đặt giá thầu thành công")
    void testPlaceBidSuccess() {
        Item item = new Electronics("Laptop", "MacBook", 1000.0, 12);
        Seller seller = new Seller("s1", "p1");
        Auction auction = new Auction("AUC-1", item, seller, 1000.0, 100.0, LocalDateTime.now().minusHours(1), LocalDateTime.now().plusHours(1));
        auction.setStatus(AuctionStatus.RUNNING);

        when(auctionRepository.findById("AUC-1")).thenReturn(auction);
        when(auctionRepository.save(any(Auction.class))).thenReturn(auction);

        boolean result = auctionService.placeBid("AUC-1", "bidder1", 1200.0);

        assertTrue(result);
        assertEquals(1200.0, auction.getCurrentPrice());
        assertEquals("bidder1", auction.getHighestBidder().getUsername());
        verify(auctionRepository).save(auction);
        verify(lockManager).lockAndRun(eq("AUC-1"), any(Runnable.class));
    }

    @Test
    @DisplayName("Kiểm thử đặt giá thầu thất bại - Không tìm thấy phiên đấu giá")
    void testPlaceBidNotFound() {
        when(auctionRepository.findById("MISSING")).thenReturn(null);
        boolean result = auctionService.placeBid("MISSING", "bidder1", 1200.0);
        assertFalse(result);
    }

    @Test
    @DisplayName("Kiểm thử đặt giá thầu thất bại - Giá không hợp lệ")
    void testPlaceBidInvalidAmount() {
        Item item = new Electronics("Laptop", "MacBook", 1000.0, 12);
        Auction auction = new Auction("AUC-1", item, new Seller("s1", ""), 1000.0, 100.0, LocalDateTime.now().minusHours(1), LocalDateTime.now().plusHours(1));
        auction.setStatus(AuctionStatus.RUNNING);

        when(auctionRepository.findById("AUC-1")).thenReturn(auction);

        boolean result = auctionService.placeBid("AUC-1", "bidder1", 500.0);

        assertFalse(result);
        verify(auctionRepository, never()).save(auction);
    }

    @Test
    @DisplayName("Kiểm thử chuyển trạng thái đấu giá")
    void testUpdateAuctionStatus() {
        Auction auction = new Auction("AUC-1", null, null, 100.0, 10.0, LocalDateTime.now(), LocalDateTime.now());
        auction.setStatus(AuctionStatus.OPEN);

        assertTrue(auctionService.updateAuctionStatus(auction, AuctionStatus.RUNNING));
        assertEquals(AuctionStatus.RUNNING, auction.getStatus());

        assertTrue(auctionService.updateAuctionStatus(auction, AuctionStatus.FINISHED));
        assertEquals(AuctionStatus.FINISHED, auction.getStatus());

        assertFalse(auctionService.updateAuctionStatus(auction, AuctionStatus.OPEN)); // RUNNING -> OPEN is invalid if looking at logic (actually current is FINISHED)
    }

    @Test
    @DisplayName("Kiểm thử các chuyển trạng thái hợp lệ và không hợp lệ")
    void testStatusTransitions() {
        Auction auction = new Auction("AUC-1", null, null, 0, 0, null, null);

        // OPEN -> RUNNING (Valid)
        auction.setStatus(AuctionStatus.OPEN);
        assertTrue(auctionService.updateAuctionStatus(auction, AuctionStatus.RUNNING));

        // RUNNING -> FINISHED (Valid)
        assertTrue(auctionService.updateAuctionStatus(auction, AuctionStatus.FINISHED));

        // FINISHED -> PAID (Valid)
        assertTrue(auctionService.updateAuctionStatus(auction, AuctionStatus.PAID));

        // PAID -> ANY (Invalid)
        assertFalse(auctionService.updateAuctionStatus(auction, AuctionStatus.RUNNING));

        // ANY -> CANCELED (Valid transitions from OPEN, RUNNING, FINISHED)
        auction.setStatus(AuctionStatus.OPEN);
        assertTrue(auctionService.updateAuctionStatus(auction, AuctionStatus.CANCELED));
    }
}
