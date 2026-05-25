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
                "A1", "Item", "Seller", "OPEN", "100", "10", "110", "None", "Desc", "Schedule",
                java.util.Collections.<String>emptyList(), // Ép kiểu rõ ràng cho danh sách rỗng
                "Điện tử",               // Tham số 12: itemType giả lập
                "Bảo hành (tháng): 12"   // Tham số 13: extraInfo giả lập
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

        // --- BỔ SUNG 2 LỆNH KIỂM TRA CHO 2 TRƯỜNG MỚI ---
        assertEquals("Điện tử", vm.itemType());
        assertEquals("Bảo hành (tháng): 12", vm.extraInfo());
    }
}
