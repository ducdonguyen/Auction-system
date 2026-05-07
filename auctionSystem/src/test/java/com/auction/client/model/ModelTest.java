package com.auction.client.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;

class ModelTest {

    @Test
    @DisplayName("Kiểm thử AuctionRoomViewModel")
    void testAuctionRoomViewModel() {
        AuctionRoomViewModel vm = new AuctionRoomViewModel(
                "A1", "Item", "Seller", "OPEN", "100", "10", "110", "None", "Desc", "Schedule", Collections.emptyList()
        );
        assertEquals("A1", vm.auctionId());
        assertEquals("Item", vm.itemName());
        assertEquals("Seller", vm.sellerName());
        assertEquals("OPEN", vm.status());
        assertEquals("100", vm.currentPrice());
        assertEquals("10", vm.stepPrice());
        assertEquals("110", vm.minimumBid());
        assertEquals("None", vm.highestBidder());
        assertEquals("Desc", vm.description());
        assertEquals("Schedule", vm.schedule());
        assertTrue(vm.bidHistory().isEmpty());
    }
}
