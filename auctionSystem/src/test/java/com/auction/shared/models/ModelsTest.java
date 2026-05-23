package com.auction.shared.models;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

class ModelsTest {

    @Test
    @DisplayName("Kiểm thử Bidder")
    void testBidder() {
        Bidder bidder = new Bidder("user1", "pass1", 1000.0);
        assertEquals("user1", bidder.getUsername());
        assertEquals("pass1", bidder.getPassword());
        assertEquals(1000.0, bidder.getBalance());
        assertNotNull(bidder.getId());

        bidder.setUsername("user2");
        bidder.setPassword("pass2");
        bidder.setBalance(2000.0);
        assertEquals("user2", bidder.getUsername());
        assertEquals("pass2", bidder.getPassword());
        assertEquals(2000.0, bidder.getBalance());

        bidder.deposit(500.0);
        assertEquals(2500.0, bidder.getBalance());
        bidder.deposit(-100.0);
        assertEquals(2500.0, bidder.getBalance());
    }

    @Test
    @DisplayName("Kiểm thử Seller")
    void testSeller() {
        Seller seller = new Seller("seller1", "spass");
        assertEquals("seller1", seller.getUsername());
        assertEquals(5.0, seller.getRating());

        seller.setRating(4.5);
        assertEquals(4.5, seller.getRating());
    }

    @Test
    @DisplayName("Kiểm thử Electronics")
    void testElectronics() {
        Electronics e = new Electronics("Phone", "iPhone", 1200.0, 24);
        assertEquals("Phone", e.getName());
        assertEquals("iPhone", e.getDescription());
        assertEquals(1200.0, e.getStartingPrice());
        assertEquals("24", e.getExtraInfo());
        assertEquals("ELECTRONICS", e.getItemType());

        e.setName("Tablet");
        e.setDescription("iPad");
        e.setStartingPrice(800.0);
        e.setWarrantyMonths(12);
        assertEquals("Tablet", e.getName());
        assertEquals("iPad", e.getDescription());
        assertEquals(800.0, e.getStartingPrice());
        assertEquals("12", e.getExtraInfo());
    }

    @Test
    @DisplayName("Kiểm thử Art")
    void testArt() {
        Art art = new Art("Painting", "Mona Lisa", 1000000.0, "Da Vinci");
        assertEquals("Da Vinci", art.getExtraInfo());
        assertEquals("ART", art.getItemType());

        art.setAuthor("Van Gogh");
        assertEquals("Van Gogh", art.getExtraInfo());
    }

    @Test
    @DisplayName("Kiểm thử Vehicle")
    void testVehicle() {
        Vehicle v = new Vehicle("Car", "Sedan", 20000.0, "Toyota");
        assertEquals("Toyota", v.getExtraInfo());
        assertEquals("VEHICLE", v.getItemType());

        v.setBrand("Honda");
        assertEquals("Honda", v.getExtraInfo());
    }

    @Test
    @DisplayName("Kiểm thử AuthUser")
    void testAuthUser() {
        AuthUser au = new AuthUser("Full Name", "user", "email@test.com", "hash", "BIDDER");
        au.setId(10L);
        assertEquals(10L, au.getId());
        assertEquals("Full Name", au.getFullName());
        assertEquals("user", au.getUsername());
        assertEquals("email@test.com", au.getEmail());
        assertEquals("hash", au.getPasswordHash());

        au.setFullName("New Name");
        au.setUsername("newuser");
        au.setEmail("new@test.com");
        au.setPasswordHash("newhash");
        assertEquals("New Name", au.getFullName());
        assertEquals("newuser", au.getUsername());
        assertEquals("new@test.com", au.getEmail());
        assertEquals("newhash", au.getPasswordHash());
        
        AuthUser empty = new AuthUser();
        assertNull(empty.getId());
    }

    @Test
    @DisplayName("Kiểm thử AuctionStatus Enum")
    void testAuctionStatus() {
        for (AuctionStatus status : AuctionStatus.values()) {
            assertNotNull(AuctionStatus.valueOf(status.name()));
        }
    }

    @Test
    @DisplayName("Kiểm thử BidTransaction")
    void testBidTransaction() {
        Bidder bidder = new Bidder("b1", "p", 100.0);
        LocalDateTime now = LocalDateTime.now();
        BidTransaction bt = new BidTransaction("TX1", bidder, 150.0, now);

        assertEquals("TX1", bt.transactionId());
        assertEquals(bidder, bt.bidder());
        assertEquals(150.0, bt.bidAmount());
        assertEquals(now, bt.timestamp());
    }
}
