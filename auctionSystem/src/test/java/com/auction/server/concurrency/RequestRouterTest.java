package com.auction.server.concurrency;

import com.auction.server.core.AuctionService;
import com.auction.server.service.AuthService;
import com.auction.shared.models.auction.Auction;
import com.auction.shared.models.auction.AuctionStatus;
import com.auction.shared.models.auction.BidTransaction;
import com.auction.shared.models.auth.Bidder;
import com.auction.shared.models.auth.Seller;
import com.auction.shared.models.auth.UserAccount;
import com.auction.shared.models.item.Item;
import com.auction.shared.network.requests.*;
import com.auction.shared.network.responses.ServiceResult;
import com.auction.shared.network.responses.TopUpResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.io.ObjectOutputStream;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class RequestRouterTest {

    @Mock
    private AuthService authService;

    @Mock
    private AuctionService auctionService;

    @Mock
    private ClientHandler handler;

    @Mock
    private ObjectOutputStream out;

    private RequestRouter requestRouter;

    @BeforeEach
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        requestRouter = new RequestRouter(authService, auctionService);
    }

    // ==========================================
    // 1. TEST LOGIN REQUEST
    // ==========================================
    @Test
    public void testRouteLoginRequest_Success() throws Exception {
        LoginRequest request = new LoginRequest("user1", "pass1");
        UserAccount mockUser = new UserAccount(1L, "User One", "user1", "user1@gmail.com", "hashed_pass", "BIDDER", 0.0);
        ServiceResult<UserAccount> mockResult = new ServiceResult<>(true, "Login success", mockUser, System.currentTimeMillis());

        when(authService.login(request)).thenReturn(mockResult);

        requestRouter.route(request, handler, out);

        verify(handler).setUsername("user1");
        verify(out).writeObject(any());
    }

    @Test
    public void testRouteLoginRequest_Failure() throws Exception {
        LoginRequest request = new LoginRequest("user1", "wrong_pass");
        ServiceResult<UserAccount> mockResult = new ServiceResult<>(false, "Login failed", null, System.currentTimeMillis());

        when(authService.login(request)).thenReturn(mockResult);

        requestRouter.route(request, handler, out);

        verify(handler, never()).setUsername(anyString());
        verify(out).writeObject(any());
    }

    // ==========================================
    // 2. TEST REGISTER REQUEST
    // ==========================================
    @Test
    public void testRouteRegistrationRequest() throws Exception {
        RegistrationRequest request = new RegistrationRequest("user2", "pass2", "User Two", "user2@gmail.com");
        ServiceResult<UserAccount> mockResult = new ServiceResult<>(true, "Register success", null, System.currentTimeMillis());

        when(authService.register(request)).thenReturn(mockResult);

        requestRouter.route(request, handler, out);

        verify(out).writeObject(any());
    }

    // ==========================================
    // 3. TEST BID REQUEST
    // ==========================================
    @Test
    public void testRouteBidRequest_Success() throws Exception {
        BidRequest request = new BidRequest("AUC001", "user1", 1000.0);
        when(auctionService.placeBid("AUC001", "user1", 1000.0)).thenReturn(true);

        requestRouter.route(request, handler, out);

        ArgumentCaptor<ServiceResult> captor = ArgumentCaptor.forClass(ServiceResult.class);
        verify(out).writeObject(captor.capture());
        assertTrue(captor.getValue().success());
        assertEquals("Đặt giá thầu thành công!", captor.getValue().message());
    }

    @Test
    public void testRouteBidRequest_Failure_CannotPlaceBid() throws Exception {
        BidRequest request = new BidRequest("AUC001", "user1", 1000.0);
        when(auctionService.placeBid("AUC001", "user1", 1000.0)).thenReturn(false);

        requestRouter.route(request, handler, out);

        ArgumentCaptor<ServiceResult> captor = ArgumentCaptor.forClass(ServiceResult.class);
        verify(out).writeObject(captor.capture());
        assertFalse(captor.getValue().success());
        assertEquals("Không thể đặt giá thầu.", captor.getValue().message());
    }

    @Test
    public void testRouteBidRequest_Exception_IllegalArgument() throws Exception {
        BidRequest request = new BidRequest("AUC001", "user1", 1000.0);
        when(auctionService.placeBid(anyString(), anyString(), anyDouble()))
                .thenThrow(new IllegalArgumentException("Số dư tài khoản không đủ"));

        requestRouter.route(request, handler, out);

        ArgumentCaptor<ServiceResult> captor = ArgumentCaptor.forClass(ServiceResult.class);
        verify(out).writeObject(captor.capture());
        assertFalse(captor.getValue().success());
        assertEquals("Số dư tài khoản không đủ", captor.getValue().message());
    }

    @Test
    public void testRouteBidRequest_Exception_Generic() throws Exception {
        BidRequest request = new BidRequest("AUC001", "user1", 1000.0);
        when(auctionService.placeBid(anyString(), anyString(), anyDouble()))
                .thenThrow(new RuntimeException("Database error"));

        requestRouter.route(request, handler, out);

        ArgumentCaptor<ServiceResult> captor = ArgumentCaptor.forClass(ServiceResult.class);
        verify(out).writeObject(captor.capture());
        assertFalse(captor.getValue().success());
        // Server có thể trả message thực tế từ exception (ví dụ "Database error").
        // Chỉ kiểm tra tiền tố chung "Lỗi xử lý hệ thống"
        assertTrue(captor.getValue().message().startsWith("Lỗi xử lý hệ thống"),
                () -> "Expected message to start with 'Lỗi xử lý hệ thống' but was: " + captor.getValue().message());
    }

    // ==========================================
    // 4. TEST JOIN ROOM REQUEST
    // ==========================================
    @Test
    public void testRouteJoinRoomRequest_WithOldRoomAndHistory() throws Exception {
        JoinRoomRequest request = new JoinRoomRequest("AUC002");
        when(handler.getCurrentWatchingAuctionId()).thenReturn("AUC001");

        // Thay vì dùng Object thật, ta MOCK luôn đối tượng Auction để cấu hình getter cho khỏe
        Auction mockAuction = mock(Auction.class);

        Bidder mockBidder = new Bidder("bidder1", "pass123", 5000.0);

        // Khởi tạo record BidTransaction đúng thứ tự
        BidTransaction tx = new BidTransaction("TX001", mockBidder, 1050.0, LocalDateTime.now());

        List<BidTransaction> history = new ArrayList<>();
        history.add(tx);

        // Định nghĩa hành vi cho mockAuction: Khi gọi get thì trả về dữ liệu ta muốn
        when(mockAuction.getBidHistory()).thenReturn(history);
        when(mockAuction.getHighestBidder()).thenReturn(mockBidder); // Thay thế cho hàm set bị lỗi

        when(auctionService.getAuctionById("AUC002")).thenReturn(mockAuction);
        when(authService.getFullName("bidder1")).thenReturn("Nguyen Van A");

        requestRouter.route(request, handler, out);

        verify(handler).setCurrentWatchingAuctionId("AUC002");
        assertEquals("Nguyen Van A", mockBidder.getFullName());
        verify(out).writeObject(any(ServiceResult.class));
    }

    // ==========================================
    // 5. TEST GET ALL AUCTIONS REQUEST
    // ==========================================
    @Test
    public void testRouteGetAllAuctionsRequest() throws Exception {
        GetAllAuctionsRequest request = new GetAllAuctionsRequest();
        List<Auction> mockList = new ArrayList<>();
        when(auctionService.getAllAuctions()).thenReturn(mockList);

        requestRouter.route(request, handler, out);

        ArgumentCaptor<ServiceResult> captor = ArgumentCaptor.forClass(ServiceResult.class);
        verify(out).writeObject(captor.capture());
        assertTrue(captor.getValue().success());
        assertEquals(mockList, captor.getValue().data());
    }

    // ==========================================
    // 6. TEST CANCEL AUCTION REQUEST
    // ==========================================
    @Test
    public void testRouteCancelAuctionRequest_Success() throws Exception {
        CancelAuctionRequest request = new CancelAuctionRequest("AUC001");

        requestRouter.route(request, handler, out);

        verify(auctionService).cancelAuction("AUC001");
        ArgumentCaptor<ServiceResult> captor = ArgumentCaptor.forClass(ServiceResult.class);
        verify(out).writeObject(captor.capture());
        assertTrue(captor.getValue().success());
    }

    @Test
    public void testRouteCancelAuctionRequest_Exception() throws Exception {
        CancelAuctionRequest request = new CancelAuctionRequest("AUC001");
        doThrow(new RuntimeException("Cannot cancel")).when(auctionService).cancelAuction("AUC001");

        requestRouter.route(request, handler, out);

        ArgumentCaptor<ServiceResult> captor = ArgumentCaptor.forClass(ServiceResult.class);
        verify(out).writeObject(captor.capture());
        assertFalse(captor.getValue().success());
        assertTrue(captor.getValue().message().contains("Lỗi hủy phiên"));
    }

    // ==========================================
    // 7. TEST CREATE AUCTION REQUEST
    // ==========================================
    @Test
    public void testRouteCreateAuctionRequest_Success() throws Exception {
        // Khởi tạo chính xác Constructor 9 tham số của CreateAuctionRequest
        CreateAuctionRequest request = new CreateAuctionRequest(
                "Macbook Pro M1",
                "16GB RAM, 512GB SSD",
                1500.0,
                100.0,
                "ELECTRONICS",
                "12", // SỬA TẠI ĐÂY: Truyền số "12" đại diện cho số tháng bảo hành để ItemFactory parse thành công
                "SELLER01",
                LocalDateTime.now(),
                LocalDateTime.now().plusDays(2)
        );
        Item mockItem = mock(Item.class);
        Seller mockSeller = mock(Seller.class);
        Auction mockAuction = new Auction("AUC100", mockItem, mockSeller, 1500.0, 100.0, LocalDateTime.now(), LocalDateTime.now().plusDays(2));

        when(auctionService.createAuction(any(), any(), anyDouble(), anyDouble(), any(), any()))
                .thenReturn(mockAuction);

        requestRouter.route(request, handler, out);

        ArgumentCaptor<ServiceResult> captor = ArgumentCaptor.forClass(ServiceResult.class);
        verify(out).writeObject(captor.capture());
        assertTrue(captor.getValue().success()); // Sẽ trả về true và pass mượt mà!
    }

    @Test
    public void testRouteCreateAuctionRequest_AnonymousSeller_And_Fail() throws Exception {
        // Truyền tham số sellerUsername (tham số thứ 7) là một chuỗi trống "" để đi vào nhánh Ẩn danh
        CreateAuctionRequest request = new CreateAuctionRequest(
                "Tranh sơn mài", "Tác phẩm cổ điển", 500.0, 50.0,
                "ART", "Tác giả: Ẩn danh", "",
                LocalDateTime.now(), LocalDateTime.now().plusDays(2)
        );
        when(auctionService.createAuction(any(), any(), anyDouble(), anyDouble(), any(), any()))
                .thenReturn(null);

        requestRouter.route(request, handler, out);

        ArgumentCaptor<ServiceResult> captor = ArgumentCaptor.forClass(ServiceResult.class);
        verify(out).writeObject(captor.capture());
        assertFalse(captor.getValue().success());
        assertEquals("Hệ thống không thể khởi tạo phiên đấu giá vào lúc này.", captor.getValue().message());
    }

    @Test
    public void testRouteCreateAuctionRequest_Exception() throws Exception {
        CreateAuctionRequest request = new CreateAuctionRequest(
                "Macbook Pro M1", "16GB RAM", 1500.0, 100.0,
                "ELECTRONICS", "Bảo hành 12 tháng", "SELLER01",
                LocalDateTime.now(), LocalDateTime.now().plusDays(2)
        );
        when(auctionService.createAuction(any(), any(), anyDouble(), anyDouble(), any(), any()))
                .thenThrow(new RuntimeException("Factory error"));

        requestRouter.route(request, handler, out);

        ArgumentCaptor<ServiceResult> captor = ArgumentCaptor.forClass(ServiceResult.class);
        verify(out).writeObject(captor.capture());
        assertFalse(captor.getValue().success());
        assertTrue(captor.getValue().message().contains("Lỗi Server"));
    }

    // ==========================================
    // 8. TEST GET PENDING AUCTIONS REQUEST
    // ==========================================
    @Test
    public void testRouteGetPendingAuctions_Success() throws Exception {
        GetPendingAuctionsRequest request = new GetPendingAuctionsRequest();
        when(auctionService.getPendingAuctions()).thenReturn(new ArrayList<>());

        requestRouter.route(request, handler, out);

        verify(out).writeObject(any(ServiceResult.class));
    }

    @Test
    public void testRouteGetPendingAuctions_Exception() throws Exception {
        GetPendingAuctionsRequest request = new GetPendingAuctionsRequest();
        when(auctionService.getPendingAuctions()).thenThrow(new RuntimeException("DB error"));

        requestRouter.route(request, handler, out);

        ArgumentCaptor<ServiceResult> captor = ArgumentCaptor.forClass(ServiceResult.class);
        verify(out).writeObject(captor.capture());
        assertFalse(captor.getValue().success());
    }

    // ==========================================
    // 9. TEST APPROVE AUCTION REQUEST
    // ==========================================
    @Test
    public void testRouteApproveAuction_Success() throws Exception {
        ApproveAuctionRequest request = new ApproveAuctionRequest("AUC001");
        Item mockItem = mock(Item.class);
        Seller mockSeller = mock(Seller.class);
        Auction mockAuction = new Auction("AUC001", mockItem, mockSeller, 1000.0, 50.0, LocalDateTime.now(), LocalDateTime.now().plusDays(1));

        when(auctionService.getAuctionById("AUC001")).thenReturn(mockAuction);
        when(auctionService.updateAuctionStatus(mockAuction, AuctionStatus.OPEN)).thenReturn(true);

        requestRouter.route(request, handler, out);

        ArgumentCaptor<ServiceResult> captor = ArgumentCaptor.forClass(ServiceResult.class);
        verify(out).writeObject(captor.capture());
        assertTrue(captor.getValue().success());
    }

    @Test
    public void testRouteApproveAuction_NotFound() throws Exception {
        ApproveAuctionRequest request = new ApproveAuctionRequest("AUC999");
        when(auctionService.getAuctionById("AUC999")).thenReturn(null);

        requestRouter.route(request, handler, out);

        ArgumentCaptor<ServiceResult> captor = ArgumentCaptor.forClass(ServiceResult.class);
        verify(out).writeObject(captor.capture());
        assertFalse(captor.getValue().success());
        assertEquals("Không tìm thấy ID phiên đấu giá", captor.getValue().message());
    }

    @Test
    public void testRouteApproveAuction_StatusUpdateFailed() throws Exception {
        ApproveAuctionRequest request = new ApproveAuctionRequest("AUC001");
        Item mockItem = mock(Item.class);
        Seller mockSeller = mock(Seller.class);
        Auction mockAuction = new Auction("AUC001", mockItem, mockSeller, 1000.0, 50.0, LocalDateTime.now(), LocalDateTime.now().plusDays(1));

        when(auctionService.getAuctionById("AUC001")).thenReturn(mockAuction);
        when(auctionService.updateAuctionStatus(mockAuction, AuctionStatus.OPEN)).thenReturn(false);

        requestRouter.route(request, handler, out);

        ArgumentCaptor<ServiceResult> captor = ArgumentCaptor.forClass(ServiceResult.class);
        verify(out).writeObject(captor.capture());
        assertFalse(captor.getValue().success());
        assertEquals("Trạng thái hiện tại không cho phép duyệt.", captor.getValue().message());
    }

    // ==========================================
    // 10. TEST TOPUP REQUEST
    // ==========================================
    @Test
    public void testRouteTopUpRequest_Success() throws Exception {
        TopUpRequest request = new TopUpRequest("user1", 50000.0);
        when(authService.topUpBalance("user1", 50000.0)).thenReturn(150000.0);

        requestRouter.route(request, handler, out);

        ArgumentCaptor<TopUpResponse> captor = ArgumentCaptor.forClass(TopUpResponse.class);
        verify(out).writeObject(captor.capture());
        assertTrue(captor.getValue().isSuccess());
        assertEquals(150000.0, captor.getValue().getNewBalance());
    }

    @Test
    public void testRouteTopUpRequest_Exception() throws Exception {
        TopUpRequest request = new TopUpRequest("user1", 50000.0);
        when(authService.topUpBalance("user1", 50000.0)).thenThrow(new RuntimeException("Topup error"));

        requestRouter.route(request, handler, out);

        ArgumentCaptor<TopUpResponse> captor = ArgumentCaptor.forClass(TopUpResponse.class);
        verify(out).writeObject(captor.capture());
        assertFalse(captor.getValue().isSuccess());
    }

    // ==========================================
    // 11. UNKNOWN & ROUTE EXCEPTION
    // ==========================================
    @Test
    public void testRouteUnknownRequest() throws Exception {
        Object unknownRequest = new Object();
        requestRouter.route(unknownRequest, handler, out);
        verify(out, never()).writeObject(any());
    }

    @Test
    public void testRoute_GlobalExceptionCatch() throws Exception {
        requestRouter.route(null, handler, out);
        verify(out, never()).writeObject(any());
    }
}