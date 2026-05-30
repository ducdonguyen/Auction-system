package com.auction.server.core;

import com.auction.server.concurrency.AuctionLockManager;
import com.auction.server.repository.AuctionRepository;
import com.auction.server.service.AuthService;
import com.auction.shared.models.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

class AutoBidManagerTest {

    private AuctionService auctionService;
    private AuctionLockManager lockManager;
    private AuctionRepository auctionRepository;
    private AuthService authService;

    @BeforeEach
    void setUp() throws Exception {
        lockManager = spy(new AuctionLockManager());
        auctionRepository = mock(AuctionRepository.class);
        authService = mock(AuthService.class);

        auctionService = new AuctionService(lockManager, auctionRepository);

        // Sử dụng Reflection để "tiêm" authService giả lập vào bên trong auctionService
        Field authField = AuctionService.class.getDeclaredField("authService");
        authField.setAccessible(true);
        authField.set(auctionService, authService);

        // Mặc định cho số dư lớn để các test chạy không bị lỗi số dư
        when(authService.getBalance(anyString())).thenReturn(1000000.0);
        when(authService.getFullName(anyString())).thenAnswer(invocation -> "Fullname of " + invocation.getArgument(0));
    }

    @Test
    @DisplayName("Kiểm thử độ ưu tiên của hàng đợi Auto-bid")
    void testAutoBidRequestPriority() throws InterruptedException {
        // Cài giá trần khác nhau: maxBid cao hơn phải đứng trước
        AutoBidRequest req1 = new AutoBidRequest("user1", 1500.0);
        AutoBidRequest req2 = new AutoBidRequest("user2", 1800.0);
        
        // So sánh: req2 có maxBid lớn hơn nên phải được xếp trước (tức là so sánh trả về số âm khi đứng trước)
        assertTrue(req2.compareTo(req1) < 0);
        assertTrue(req1.compareTo(req2) > 0);

        // Cài giá trần giống nhau: cài trước (setupTime nhỏ hơn) phải đứng trước
        AutoBidRequest req3 = new AutoBidRequest("user3", 1500.0);
        // Ngủ 15 mili-giây để đảm bảo thời gian tạo khác biệt rõ ràng
        Thread.sleep(15);
        AutoBidRequest req4 = new AutoBidRequest("user4", 1500.0);

        assertTrue(req3.compareTo(req4) < 0);
        assertTrue(req4.compareTo(req3) > 0);
    }

    @Test
    @DisplayName("Kiểm thử 1 Bot Auto-bid hoạt động khi có người đặt giá mới")
    void testSingleBotAutoBid() {
        Item item = new Electronics("Laptop", "MacBook", 1000.0, 12);
        Seller seller = new Seller("s1", "p1");
        Auction auction = new Auction("AUC-BOT-1", item, seller, 1000.0, 100.0,
                LocalDateTime.now().minusHours(1), LocalDateTime.now().plusHours(1));
        auction.setStatus(AuctionStatus.RUNNING);

        when(auctionRepository.findById("AUC-BOT-1")).thenReturn(auction);
        when(auctionRepository.save(any(Auction.class))).thenReturn(auction);

        // Đăng ký auto-bid cho bidder1 với maxBid = 1500.0
        // Hệ thống tự động đặt giá khởi điểm 1100.0 cho bidder1 ngay khi đăng ký
        auctionService.registerAutoBid("AUC-BOT-1", "bidder1", 1500.0);

        // Kiểm tra xem bidder1 đã ở trong danh sách chưa
        List<AutoBidRequest> list = auctionService.getAutoBids("AUC-BOT-1");
        assertEquals(1, list.size());
        assertEquals("bidder1", list.get(0).getBidderUsername());

        // Hiện tại giá là 1100.0. bidder2 đặt giá thủ công 1200.0
        // Sau khi đặt giá thành công, trigger sẽ chạy và tự động đặt giá 1300.0 cho bidder1
        boolean result = auctionService.placeBid("AUC-BOT-1", "bidder2", 1200.0);

        assertTrue(result);
        // Giá hiện tại phải được tự động đẩy lên 1300.0 bởi bidder1
        assertEquals(1300.0, auction.getCurrentPrice());
        assertEquals("bidder1", auction.getHighestBidder().getUsername());
    }

    @Test
    @DisplayName("Kiểm thử Chiến tranh Bot giữa 2 Bot Auto-bid")
    void testBotWarScenario() {
        Item item = new Electronics("Laptop", "MacBook", 1000.0, 12);
        Seller seller = new Seller("s1", "p1");
        Auction auction = new Auction("AUC-WAR", item, seller, 1000.0, 100.0,
                LocalDateTime.now().minusHours(1), LocalDateTime.now().plusHours(1));
        auction.setStatus(AuctionStatus.RUNNING);

        when(auctionRepository.findById("AUC-WAR")).thenReturn(auction);
        when(auctionRepository.save(any(Auction.class))).thenReturn(auction);

        // Đăng ký Bot A: max 1500.0 -> tự động bid 1100.0
        auctionService.registerAutoBid("AUC-WAR", "botA", 1500.0);

        // Đăng ký Bot B: max 1800.0 -> tự động bid 1200.0 -> Bot A bid 1300.0 -> Bot B bid 1400.0 -> Bot A bid 1500.0 -> Bot B bid 1600.0
        auctionService.registerAutoBid("AUC-WAR", "botB", 1800.0);

        // Cả 2 bot tự động đấu giá đẩy giá lên 1600.0
        assertEquals(1600.0, auction.getCurrentPrice());
        assertEquals("botB", auction.getHighestBidder().getUsername());
    }

    @Test
    @DisplayName("Kiểm thử khi Bot không đủ số dư để đặt giá")
    void testBotInsufficientBalance() {
        Item item = new Electronics("Laptop", "MacBook", 1000.0, 12);
        Seller seller = new Seller("s1", "p1");
        Auction auction = new Auction("AUC-NO-MONEY", item, seller, 1000.0, 100.0,
                LocalDateTime.now().minusHours(1), LocalDateTime.now().plusHours(1));
        auction.setStatus(AuctionStatus.RUNNING);

        when(auctionRepository.findById("AUC-NO-MONEY")).thenReturn(auction);
        when(auctionRepository.save(any(Auction.class))).thenReturn(auction);

        // Bot A: max 1500.0 nhưng không có tiền (số dư = 0)
        when(authService.getBalance("poorBot")).thenReturn(0.0);
        auctionService.registerAutoBid("AUC-NO-MONEY", "poorBot", 1500.0);

        // poorBot không đủ tiền nên giá khởi điểm vẫn là 1000.0, chưa có ai trả giá cao nhất
        assertEquals(1000.0, auction.getCurrentPrice());
        assertNull(auction.getHighestBidder());

        // poorBot phải bị xóa khỏi hàng đợi Auto-bid
        List<AutoBidRequest> list = auctionService.getAutoBids("AUC-NO-MONEY");
        assertTrue(list.isEmpty());
    }
}
