package com.auction.shared;

import com.auction.shared.network.*;
import com.auction.shared.exceptions.*;
import com.auction.shared.models.Bidder;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

class SharedMiscTest {

    @Test
    @DisplayName("Kiểm thử Network Requests")
    void testNetworkRequests() {
        BidRequest br = new BidRequest("A1", "B1", 100.0);
        assertEquals("A1", br.getAuctionId());
        assertEquals("B1", br.getBidderName());
        assertEquals(100.0, br.getAmount());

        JoinRoomRequest jrr = new JoinRoomRequest("A2");
        assertEquals("A2", jrr.getAuctionId());

        LoginRequest lr = new LoginRequest("user", "pass");
        assertEquals("user", lr.username());
        assertEquals("pass", lr.password());

        RegistrationRequest rr = new RegistrationRequest("Full", "u", "e", "p");
        assertEquals("Full", rr.fullName());
        assertEquals("u", rr.username());
        assertEquals("e", rr.email());
        assertEquals("p", rr.password());

        ServiceResult<String> sr = new ServiceResult<>(true, "msg", "data");
        assertTrue(sr.success());
        assertEquals("msg", sr.message());
        assertEquals("data", sr.data());
    }

    @Test
    @DisplayName("Kiểm thử Exceptions")
    void testExceptions() {
        assertThrows(AuctionClosedException.class, () -> {
            throw new AuctionClosedException("Closed");
        });
        assertThrows(InvalidBidException.class, () -> {
            throw new InvalidBidException("Invalid");
        });
        
        AuctionClosedException ace = new AuctionClosedException("msg");
        assertEquals("msg", ace.getMessage());

        InvalidBidException ibe = new InvalidBidException("msg");
        assertEquals("msg", ibe.getMessage());
    }
}
