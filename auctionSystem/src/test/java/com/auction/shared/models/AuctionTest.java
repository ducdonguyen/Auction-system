package com.auction.shared.models;

import com.auction.shared.exceptions.AuctionClosedException;
import com.auction.shared.exceptions.InvalidBidException;
import com.auction.shared.models.auction.Auction;
import com.auction.shared.models.auction.AuctionStatus;
import com.auction.shared.models.auction.BidTransaction;
import com.auction.shared.models.auth.Bidder;
import com.auction.shared.models.auth.Seller;
import com.auction.shared.models.item.Art;
import com.auction.shared.models.item.Electronics;
import com.auction.shared.models.item.Item;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

class AuctionTest {

    private Auction auction;
    private Item item;
    private Seller seller;
    private LocalDateTime now;

    @BeforeEach
    void setUp() {
        item = new Electronics("Laptop", "MacBook Pro", 1000.0, 12);
        seller = new Seller("seller1", "hash");
        now = LocalDateTime.now();
        auction = new Auction("AUC-1", item, seller, 1000.0, 100.0, now.minusHours(1), now.plusHours(1));
    }

    @Test
    @DisplayName("Kiểm thử validate đặt giá khi đấu giá chưa bắt đầu")
    void testValidateBidNotRunning() {
        auction.setStatus(AuctionStatus.OPEN);
        assertThrows(AuctionClosedException.class, () -> auction.validateBid(1200.0));
    }

    @Test
    @DisplayName("Kiểm thử validate đặt giá khi giá <= 0")
    void testValidateBidNegative() {
        auction.setStatus(AuctionStatus.RUNNING);
        assertThrows(InvalidBidException.class, () -> auction.validateBid(-10.0));
        assertThrows(InvalidBidException.class, () -> auction.validateBid(0));
    }

    @Test
    @DisplayName("Kiểm thử validate đặt giá khi giá thấp hơn bước nhảy")
    void testValidateBidTooLow() {
        auction.setStatus(AuctionStatus.RUNNING);
        // Current price 1000, step 100 -> min bid 1100
        assertThrows(InvalidBidException.class, () -> auction.validateBid(1050.0));
    }

    @Test
    @DisplayName("Kiểm thử validate đặt giá thành công")
    void testValidateBidSuccess() throws Exception {
        auction.setStatus(AuctionStatus.RUNNING);
        assertTrue(auction.validateBid(1100.0));
        assertTrue(auction.validateBid(2000.0));
    }

    @Test
    @DisplayName("Kiểm thử cập nhật trạng thái đấu giá")
    void testUpdateAuctionState() {
        Bidder bidder = new Bidder("bidder1", "hash", 5000.0);
        double amount = 1500.0;
        BidTransaction transaction = new BidTransaction("T-1", bidder, amount, LocalDateTime.now());

        auction.updateAuctionState(bidder, amount, transaction);

        assertEquals(amount, auction.getCurrentPrice());
        assertEquals(bidder, auction.getHighestBidder());
        assertEquals(1, auction.getBidHistory().size());
        assertEquals(transaction, auction.getBidHistory().get(0));
    }

    @Test
    @DisplayName("Kiểm thử danh sách lịch sử đấu giá không thể sửa đổi trực tiếp")
    void testBidHistoryUnmodifiable() {
        assertThrows(UnsupportedOperationException.class, () -> {
            auction.getBidHistory().add(null);
        });
    }

    @Test
    @DisplayName("Kiểm thử các getter/setter của Auction")
    void testGettersSetters() {
        auction.setAuctionId("NEW-ID");
        assertEquals("NEW-ID", auction.getAuctionId());

        Item newItem = new Art("Painting", "Mona Lisa", 5000.0, "Da Vinci");
        auction.setItem(newItem);
        assertEquals(newItem, auction.getItem());

        Seller newSeller = new Seller("s2", "h");
        auction.setSeller(newSeller);
        assertEquals(newSeller, auction.getSeller());

        auction.setStatus(AuctionStatus.FINISHED);
        assertEquals(AuctionStatus.FINISHED, auction.getStatus());

        LocalDateTime st = LocalDateTime.now().plusDays(1);
        auction.setStartTime(st);
        assertEquals(st, auction.getStartTime());

        LocalDateTime et = LocalDateTime.now().plusDays(2);
        auction.setEndTime(et);
        assertEquals(et, auction.getEndTime());
    }
}
