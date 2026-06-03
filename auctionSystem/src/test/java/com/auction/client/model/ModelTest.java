package com.auction.client.model;

import org.junit.jupiter.api.Test;
import java.util.Collections;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class ModelTest {
    @Test
    public void testAuctionRoomViewModel() {
        AuctionRoomViewModel vm = new AuctionRoomViewModel(
                "AUC001", "Item", "Seller", "OPEN", "100", "10", "110", "None", "Desc", "Schedule",
                Collections.emptyList(), "ELECTRONICS", "None", 22
        );
        assertEquals("AUC001", vm.auctionId());
        assertEquals("Item", vm.itemName());
        assertEquals("ELECTRONICS", vm.itemType());
    }
}
