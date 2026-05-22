package com.auction.client.service;

import com.auction.shared.models.Auction;
import com.auction.shared.models.AuctionRow;
import com.auction.shared.models.AuthUser;
import com.auction.shared.network.ServiceResult;
import com.auction.client.model.AuctionRoomViewModel;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import com.auction.client.network.SocketClient;
import com.auction.shared.network.LoginRequest;
import com.auction.shared.network.RegistrationRequest;
import org.junit.jupiter.api.AfterEach;
import org.mockito.MockedStatic;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ClientServicesTest {

    private AuctionCatalogService catalogService;
    private AuctionRoomService roomService;
    private AuthService authService;
    private MockedStatic<SocketClient> mockedSocketClient;
    private SocketClient mockSocketClient;

    @BeforeEach
    void setUp() {
        catalogService = new AuctionCatalogService();
        roomService = new AuctionRoomService();
        authService = new AuthService();
        mockSocketClient = mock(SocketClient.class);
        mockedSocketClient = mockStatic(SocketClient.class);
        mockedSocketClient.when(SocketClient::getInstance).thenReturn(mockSocketClient);

        SessionContext.setCurrentUser(new AuthUser("Test User", "testuser", "test@test.com", "hash"));
    }

    @AfterEach
    void tearDown() {
        mockedSocketClient.close();
    }

    @Test
    @DisplayName("Kiểm thử AuthService login thành công")
    void testAuthServiceLogin_Success() throws Exception {
        AuthUser user = new AuthUser("Full Name", "user", "email", "hash");
        ServiceResult<AuthUser> expected = new ServiceResult<>(true, "Success", user);

        when(mockSocketClient.receiveResponse()).thenReturn(expected);

        LoginRequest request = new LoginRequest("user", "pass");
        ServiceResult<AuthUser> result = authService.login(request);

        assertTrue(result.success());
        assertEquals("user", SessionContext.getCurrentUser().getUsername());
        verify(mockSocketClient).sendRequest(request);
    }

    @Test
    @DisplayName("Kiểm thử AuthService login thất bại (thiếu thông tin)")
    void testAuthServiceLogin_MissingInfo() {
        LoginRequest request = new LoginRequest("", "");
        ServiceResult<AuthUser> result = authService.login(request);
        assertFalse(result.success());
        assertEquals("Nhập đủ thông tin.", result.message());
    }

    @Test
    @DisplayName("Kiểm thử AuthService login lỗi kết nối")
    void testAuthServiceLogin_ConnectionError() throws Exception {
        doThrow(new IOException("Conn error")).when(mockSocketClient).sendRequest(any());

        LoginRequest request = new LoginRequest("user", "pass");
        ServiceResult<AuthUser> result = authService.login(request);

        assertFalse(result.success());
        assertEquals("Lỗi kết nối.", result.message());
    }

    @Test
    @DisplayName("Kiểm thử AuthService register thành công")
    void testAuthServiceRegister_Success() throws Exception {
        RegistrationRequest request = new RegistrationRequest("Full Name", "user", "email", "pass");
        AuthUser user = new AuthUser("Full Name", "user", "email", "hash");
        ServiceResult<AuthUser> expected = new ServiceResult<>(true, "Success", user);

        when(mockSocketClient.receiveResponse()).thenReturn(expected);

        ServiceResult<AuthUser> result = authService.register(request);
        assertTrue(result.success());
        verify(mockSocketClient).sendRequest(request);
    }

    @Test
    @DisplayName("Kiểm thử AuctionCatalogService filter")
    void testFilterAuctions() throws Exception {
        // Giả lập Server trả về danh sách phòng đấu giá cho Lobby
        List<Auction> mockAuctions = AuctionDataStore.getAuctions();
        when(mockSocketClient.receiveResponse()).thenReturn(new ServiceResult<>(true, "OK", mockAuctions));

        List<AuctionRow> all = catalogService.filterAuctions("", "Tất cả");
        assertFalse(all.isEmpty());

        List<AuctionRow> filtered = catalogService.filterAuctions("VinFast", "RUNNING");
        assertFalse(filtered.isEmpty());
        assertTrue(filtered.get(0).itemName().contains("VinFast"));

        List<AuctionRow> none = catalogService.filterAuctions("NonExistentItem", "Tất cả");
        assertTrue(none.isEmpty());

        List<String> statuses = catalogService.getAvailableStatuses();
        assertTrue(statuses.contains("Tất cả"));
        assertTrue(statuses.contains("OPEN"));
    }

    @Test
    @DisplayName("Kiểm thử AuctionRoomService getAuctionRoom")
    void testGetAuctionRoom() throws Exception {
        // Mượn 1 Auction mẫu từ kho nội bộ để giả lập Server trả về
        Auction mockAuction = AuctionDataStore.findById("AUC002").orElse(null);
        when(mockSocketClient.receiveResponse()).thenReturn(new ServiceResult<>(true, "OK", mockAuction));

        Optional<ServiceResult<AuctionRoomViewModel>> result = roomService.getAuctionRoom("AUC002");
        assertTrue(result.isPresent());
        assertTrue(result.get().success());
        assertEquals("AUC002", result.get().data().auctionId());

        // Giả lập Server trả về lỗi khi phòng không tồn tại
        when(mockSocketClient.receiveResponse()).thenReturn(new ServiceResult<>(false, "Không tìm thấy phòng", null));
        Optional<ServiceResult<AuctionRoomViewModel>> notFound = roomService.getAuctionRoom("INVALID");
        assertFalse(notFound.isPresent());
    }

    @Test
    @DisplayName("Kiểm thử AuctionRoomService placeBid")
    void testPlaceBid() throws Exception {
        // Giả lập phản hồi khi Server đồng ý mức giá
        when(mockSocketClient.receiveResponse()).thenReturn(new ServiceResult<>(true, "Đã gửi lệnh đặt giá!", null));
        ServiceResult<AuctionRoomViewModel> result = roomService.placeBid("AUC002", "1,020,000,000");
        assertTrue(result.success());
        assertNull(result.data()); // Vì code Online mới không cập nhật data ở đây mà nhờ RealtimeListener

        // Giả lập phản hồi khi Server từ chối vì giá thấp
        when(mockSocketClient.receiveResponse()).thenReturn(new ServiceResult<>(false, "Giá thấp", null));
        ServiceResult<AuctionRoomViewModel> lowBid = roomService.placeBid("AUC002", "500000");
        assertFalse(lowBid.success());
        assertTrue(lowBid.message().contains("Giá thấp"));

        // Lỗi Validate nhập liệu nội bộ (Bị bắt ngay tại Client, không kịp gọi Server)
        ServiceResult<AuctionRoomViewModel> invalidAmount = roomService.placeBid("AUC002", "abc");
        assertFalse(invalidAmount.success());

        ServiceResult<AuctionRoomViewModel> emptyAmount = roomService.placeBid("AUC002", "");
        assertFalse(emptyAmount.success());
    }
}