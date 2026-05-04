package com.auction.shared.models;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ModelsTest {

    @Test
    @DisplayName("Kiểm thử Art model")
    void testArt() {
        Art art = new Art("Mona Lisa", "Da Vinci's masterpiece", 1000000.0, "Leonardo da Vinci");
        assertEquals("ART", art.getItemType());
        assertEquals("Leonardo da Vinci", art.getExtraInfo());
        art.setAuthor("Leonardo");
        assertEquals("Leonardo", art.getExtraInfo());
        
        art.setName("New Name");
        assertEquals("New Name", art.getName());
        art.setDescription("New Desc");
        assertEquals("New Desc", art.getDescription());
        art.setStartingPrice(200.0);
        assertEquals(200.0, art.getStartingPrice());
    }

    @Test
    @DisplayName("Kiểm thử Electronics model")
    void testElectronics() {
        Electronics e = new Electronics("Phone", "iPhone", 1000.0, 24);
        assertEquals("ELECTRONICS", e.getItemType());
        assertEquals("24", e.getExtraInfo());
        e.setWarrantyMonths(36);
        assertEquals("36", e.getExtraInfo());
    }

    @Test
    @DisplayName("Kiểm thử Vehicle model")
    void testVehicle() {
        Vehicle v = new Vehicle("Car", "Sedan", 20000.0, "Toyota");
        assertEquals("VEHICLE", v.getItemType());
        assertEquals("Toyota", v.getExtraInfo());
        v.setBrand("Honda");
        assertEquals("Honda", v.getExtraInfo());
    }

    @Test
    @DisplayName("Kiểm thử Bidder model")
    void testBidder() {
        Bidder b = new Bidder("bidder1", "pass", 1000.0);
        assertEquals(1000.0, b.getBalance());
        b.deposit(500.0);
        assertEquals(1500.0, b.getBalance());
        b.deposit(-100.0);
        assertEquals(1500.0, b.getBalance());
        b.setBalance(2000.0);
        assertEquals(2000.0, b.getBalance());
    }

    @Test
    @DisplayName("Kiểm thử Seller model")
    void testSeller() {
        Seller s = new Seller("seller1", "pass");
        assertEquals(5.0, s.getRating());
        s.setRating(4.5);
        assertEquals(4.5, s.getRating());
    }

    @Test
    @DisplayName("Kiểm thử Entity ID generation")
    void testEntityId() {
        Art art1 = new Art("A", "D", 1.0, "Au");
        Art art2 = new Art("A", "D", 1.0, "Au");
        assertNotNull(art1.getId());
        assertNotEquals(art1.getId(), art2.getId());
    }
}
