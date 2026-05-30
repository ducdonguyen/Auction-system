package com.auction.shared.models;

import com.auction.shared.models.item.Art;
import com.auction.shared.models.item.Electronics;
import com.auction.shared.models.item.Vehicle;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ItemTest {

    @Test
    void testArt() {
        Art art = new Art("Mona Lisa", "Classic", 1000000.0, "Da Vinci");
        assertEquals("Mona Lisa", art.getName());
        assertEquals("Classic", art.getDescription());
        assertEquals(1000000.0, art.getStartingPrice());
        assertEquals("Da Vinci", art.getExtraInfo());
        assertEquals("ART", art.getItemType());

        art.setAuthor("Someone else");
        assertEquals("Someone else", art.getExtraInfo());
    }

    @Test
    void testElectronics() {
        Electronics electronics = new Electronics("iPhone", "New", 1000.0, 12);
        assertEquals("iPhone", electronics.getName());
        assertEquals(1000.0, electronics.getStartingPrice());
        assertEquals("12", electronics.getExtraInfo());
        assertEquals("ELECTRONICS", electronics.getItemType());

        electronics.setWarrantyMonths(24);
        assertEquals("24", electronics.getExtraInfo());
    }

    @Test
    void testVehicle() {
        Vehicle vehicle = new Vehicle("Tesla", "Electric", 50000.0, "Tesla Brand");
        assertEquals("Tesla", vehicle.getName());
        assertEquals(50000.0, vehicle.getStartingPrice());
        assertEquals("Tesla Brand", vehicle.getExtraInfo());
        assertEquals("VEHICLE", vehicle.getItemType());

        vehicle.setBrand("New Brand");
        assertEquals("New Brand", vehicle.getExtraInfo());
    }
}
