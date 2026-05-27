package com.auction.server.concurrency;

import com.auction.server.core.AuctionService;
import com.auction.server.service.AuthService;
import com.auction.shared.models.AuthUser;
import com.auction.shared.network.BidRequest;
import com.auction.shared.network.JoinRoomRequest;
import com.auction.shared.network.LoginRequest;
import com.auction.shared.network.RegistrationRequest;
import com.auction.shared.network.ServiceResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.io.ObjectOutputStream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class RequestRouterTest {

    // Khai báo các đối tượng giả lập (Mock)
    @Mock
    private AuthService authService;

    @Mock
    private AuctionService auctionService;

    @Mock
    private ClientHandler handler;

    @Mock
    private ObjectOutputStream out;

    // Đối tượng thật cần được test
    private RequestRouter requestRouter;

    @BeforeEach
    public void setUp() {
        // Khởi tạo các mock object
        MockitoAnnotations.openMocks(this);

        // Tiêm các dependency giả lập vào RequestRouter
        requestRouter = new RequestRouter(authService, auctionService);
    }

    @Test
    public void testRouteBidRequestSuccess() throws Exception {
        BidRequest request = new BidRequest("AUC001", "user1", 1000.0);
        when(auctionService.placeBid("AUC001", "user1", 1000.0)).thenReturn(true);

        requestRouter.route(request, handler, out);

        ArgumentCaptor<ServiceResult> captor = ArgumentCaptor.forClass(ServiceResult.class);
        verify(out).writeObject(captor.capture());
        ServiceResult result = captor.getValue();
        
        assertTrue(result.success());
        assertEquals("Đặt giá thầu thành công!", result.message());
    }

    @Test
    public void testRouteBidRequestFailure() throws Exception {
        BidRequest request = new BidRequest("AUC001", "user1", 1000.0);
        doThrow(new RuntimeException("Bid too low")).when(auctionService).placeBid(anyString(), anyString(), anyDouble());

        requestRouter.route(request, handler, out);

        ArgumentCaptor<ServiceResult> captor = ArgumentCaptor.forClass(ServiceResult.class);
        verify(out).writeObject(captor.capture());
        ServiceResult result = captor.getValue();
        
        assertFalse(result.success());
        assertEquals("Lỗi xử lý hệ thống: Bid too low", result.message());
    }

    @Test
    public void testRouteJoinRoomRequest() throws Exception {
        JoinRoomRequest request = new JoinRoomRequest("AUC001");

        requestRouter.route(request, handler, out);

        verify(handler).setCurrentWatchingAuctionId("AUC001");
        verify(out).writeObject(any(ServiceResult.class));
    }

    @Test
    public void testRouteUnknownRequest() throws Exception {
        Object unknownRequest = new Object();

        requestRouter.route(unknownRequest, handler, out);

        verify(out, never()).writeObject(any());
    }
}
