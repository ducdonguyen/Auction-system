package com.auction.server.core;

import com.auction.shared.exceptions.AuthenticationException;
import com.auction.shared.models.auction.Auction;
import com.auction.shared.models.auction.AuctionStatus;
import com.auction.shared.models.auction.BidTransaction;
import com.auction.shared.models.auth.Seller;
import com.auction.shared.models.item.Art;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;


@ExtendWith(MockitoExtension.class)
class AuctionManagerTest {

    private AuctionManager auctionManager;
    private Auction auction;

    @Mock
    private AuctionObserver observer;

    @BeforeEach
    void setUp() {
        auctionManager = AuctionManager.getInstance();
        Art item = new Art("Art", "Desc", 100.0, "Artist");
        Seller seller = new Seller("seller", "pass");
        auction = new Auction("TEST-AUC-1", item, seller, 100.0, 10.0, LocalDateTime.now(), LocalDateTime.now().plusHours(1));
    }

    @Test
    void testAddAuctionSuccess() throws AuthenticationException {
        auctionManager.addAuction(auction, "ADMIN_SECRET_TOKEN");
        // Kiểm tra việc add thành công (không ném exception)
    }

    @Test
    void testAddAuctionUnauthorized() {
        assertThrows(AuthenticationException.class, () -> auctionManager.addAuction(auction, "WRONG_TOKEN"));
    }

    @Test
    void testNotifyObservers() {
        auctionManager.subscribe("TEST-AUC-1", observer);

        BidTransaction bid = new BidTransaction("TX1", null, 150.0, LocalDateTime.now());
        auctionManager.notifyObservers("TEST-AUC-1", bid);

        verify(observer).updateNewBid("TEST-AUC-1", bid);

        auctionManager.notifyStatusUpdate("TEST-AUC-1", AuctionStatus.RUNNING);
        verify(observer).updateStatus("TEST-AUC-1", AuctionStatus.RUNNING);

        auctionManager.unsubscribe("TEST-AUC-1", observer);
    }
}
