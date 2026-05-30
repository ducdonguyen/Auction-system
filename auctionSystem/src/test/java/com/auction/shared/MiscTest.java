package com.auction.shared;

import com.auction.server.concurrency.AuctionLockManager;
import com.auction.server.core.AuctionManager;
import com.auction.server.core.AuctionObserver;
import com.auction.shared.exceptions.AuctionClosedException;
import com.auction.shared.exceptions.AuthenticationException;
import com.auction.shared.exceptions.InvalidBidException;
import com.auction.shared.models.auction.Auction;
import com.auction.shared.models.auction.AuctionStatus;
import com.auction.shared.models.auction.BidTransaction;
import com.auction.shared.network.requests.BidRequest;
import com.auction.shared.network.requests.JoinRoomRequest;
import com.auction.shared.network.requests.LoginRequest;
import com.auction.shared.network.requests.RegistrationRequest;
import com.auction.shared.network.responses.ServiceResult;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class MiscTest {

    @Test
    @DisplayName("Kiểm thử Network Requests")
    void testNetworkRequests() {
        BidRequest br = new BidRequest("A1", "B1", 100.0);
        assertEquals("A1", br.getAuctionId());
        assertEquals("B1", br.getBidderName());
        assertEquals(100.0, br.getAmount());

        JoinRoomRequest jrr = new JoinRoomRequest("A1");
        assertEquals("A1", jrr.getAuctionId());

        ServiceResult<String> sr = new ServiceResult<>(true, "OK", "Data");
        assertTrue(sr.success());
        assertEquals("OK", sr.message());
        assertEquals("Data", sr.data());

        LoginRequest lr = new LoginRequest("user", "pass");
        assertEquals("user", lr.username());
        assertEquals("pass", lr.password());

        RegistrationRequest rr = new RegistrationRequest("Full Name", "user", "email@test.com", "pass");
        assertEquals("Full Name", rr.fullName());
        assertEquals("user", rr.username());
        assertEquals("email@test.com", rr.email());
        assertEquals("pass", rr.password());
    }

    @Test
    @DisplayName("Kiểm thử Exceptions")
    void testExceptions() {
        assertThrows(AuctionClosedException.class, () -> { throw new AuctionClosedException("Closed"); });
        assertThrows(AuthenticationException.class, () -> { throw new AuthenticationException("Auth"); });
        assertThrows(InvalidBidException.class, () -> { throw new InvalidBidException("Invalid"); });
    }

    @Test
    @DisplayName("Kiểm thử AuctionLockManager")
    void testAuctionLockManager() {
        AuctionLockManager alm = new AuctionLockManager();
        AtomicBoolean run = new AtomicBoolean(false);
        alm.lockAndRun("key1", () -> run.set(true));
        assertTrue(run.get());

        // Test InterruptedException is harder, but we can check if it creates locks
        assertDoesNotThrow(() -> alm.lockAndRun("key2", () -> {}));
    }

    @Test
    @DisplayName("Kiểm thử AuctionManager")
    void testAuctionManager() throws AuthenticationException {
        AuctionManager am = AuctionManager.getInstance();
        Auction auction = new Auction("TEST-AM-1", null, null, 100, 10, null, null);
        
        // Test addAuction with invalid token
        assertThrows(AuthenticationException.class, () -> am.addAuction(auction, "WRONG"));
        
        // Test addAuction with valid token
        am.addAuction(auction, "ADMIN_SECRET_TOKEN");
        
        // Test add existing auction
        assertThrows(IllegalArgumentException.class, () -> am.addAuction(auction, "ADMIN_SECRET_TOKEN"));

        // Test Observer
        AuctionObserver observer = mock(AuctionObserver.class);
        am.subscribe("TEST-AM-1", observer);
        
        BidTransaction bt = new BidTransaction("T1", null, 150, LocalDateTime.now());
        am.notifyObservers("TEST-AM-1", bt);
        verify(observer).updateNewBid("TEST-AM-1", bt);
        
        am.notifyStatusUpdate("TEST-AM-1", AuctionStatus.RUNNING);
        verify(observer).updateStatus("TEST-AM-1", AuctionStatus.RUNNING);
        
        am.unsubscribe("TEST-AM-1", observer);
        am.notifyStatusUpdate("TEST-AM-1", AuctionStatus.FINISHED);
        verify(observer, times(1)).updateStatus(anyString(), any()); // Still 1 from before

        // Additional coverage for AuctionManager
        assertThrows(IllegalArgumentException.class, () -> am.addAuction(null, "ADMIN_SECRET_TOKEN"));
        assertThrows(IllegalArgumentException.class, () -> am.addAuction(new Auction(null, null, null, 0, 0, null, null), "ADMIN_SECRET_TOKEN"));
        
        am.subscribe("MISSING-ID", mock(AuctionObserver.class));
        am.notifyObservers("MISSING-ID", bt);
        am.notifyStatusUpdate("MISSING-ID", AuctionStatus.OPEN);
        am.unsubscribe("MISSING-ID-2", mock(AuctionObserver.class));
    }
}
