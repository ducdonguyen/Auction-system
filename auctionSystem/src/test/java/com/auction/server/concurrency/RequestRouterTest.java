package com.auction.server.concurrency;

import com.auction.server.core.AuctionService;
import com.auction.shared.models.AuthUser;
import com.auction.shared.network.BidRequest;
import com.auction.shared.network.JoinRoomRequest;
import com.auction.shared.network.LoginRequest;
import com.auction.shared.network.RegistrationRequest;
import com.auction.shared.network.ServiceResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.io.ObjectOutputStream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class RequestRouterTest {

    private ClientHandler handler;
    private ObjectOutputStream out;
    private AuctionService auctionService;

    @BeforeEach
    public void setUp() {
        handler = mock(ClientHandler.class);
        out = mock(ObjectOutputStream.class);
        auctionService = mock(AuctionService.class);
    }

    @Test
    public void testRouteBidRequestSuccess() throws Exception {
        BidRequest request = new BidRequest("AUC001", "user1", 1000.0);
        when(auctionService.placeBid("AUC001", "user1", 1000.0)).thenReturn(true);

        RequestRouter.route(request, handler, out, auctionService);

        ArgumentCaptor<ServiceResult> captor = ArgumentCaptor.forClass(ServiceResult.class);
        verify(out).writeObject(captor.capture());
        ServiceResult result = captor.getValue();
        
        assertTrue(result.success());
        assertEquals("Bid placed successfully", result.message());
    }

    @Test
    public void testRouteBidRequestFailure() throws Exception {
        BidRequest request = new BidRequest("AUC001", "user1", 1000.0);
        doThrow(new RuntimeException("Bid too low")).when(auctionService).placeBid(anyString(), anyString(), anyDouble());

        RequestRouter.route(request, handler, out, auctionService);

        ArgumentCaptor<ServiceResult> captor = ArgumentCaptor.forClass(ServiceResult.class);
        verify(out).writeObject(captor.capture());
        ServiceResult result = captor.getValue();
        
        assertFalse(result.success());
        assertEquals("Bid too low", result.message());
    }

    @Test
    public void testRouteJoinRoomRequest() throws Exception {
        JoinRoomRequest request = new JoinRoomRequest("AUC001");
        
        RequestRouter.route(request, handler, out, auctionService);

        verify(handler).setCurrentWatchingAuctionId("AUC001");
        verify(out).writeObject(any(ServiceResult.class));
    }

    @Test
    public void testRouteUnknownRequest() throws Exception {
        Object unknownRequest = new Object();
        
        RequestRouter.route(unknownRequest, handler, out, auctionService);

        verify(out, never()).writeObject(any());
    }
}
