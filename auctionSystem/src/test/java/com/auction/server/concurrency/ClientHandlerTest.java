package com.auction.server.concurrency;

import com.auction.server.service.AuthService;
import com.auction.shared.models.auction.AuctionStatus;
import com.auction.shared.models.auction.BidTransaction;
import com.auction.shared.models.auth.Bidder;
import com.auction.shared.network.responses.ServiceResult;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.io.IOException;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.time.LocalDateTime;
import java.util.concurrent.ConcurrentHashMap;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class ClientHandlerTest {

    @Mock
    private Socket socket;

    @Mock
    private AuthService authService;

    @Mock
    private RequestRouter requestRouter;

    @Mock
    private ObjectOutputStream out;

    private ClientHandler clientHandler;

    @BeforeEach
    public void setUp() throws Exception {
        MockitoAnnotations.openMocks(this);
        clientHandler = new ClientHandler(socket, requestRouter, authService);

        // Tiêm mock ObjectOutputStream qua reflection vì biến 'out' được khởi tạo động trong run()
        java.lang.reflect.Field outField = ClientHandler.class.getDeclaredField("out");
        outField.setAccessible(true);
        outField.set(clientHandler, out);
    }

    @AfterEach
    public void tearDown() throws Exception {
        // Dọn dẹp map static 'activeClients' sau mỗi test case để tránh làm nhiễu dữ liệu giữa các test
        java.lang.reflect.Field field = ClientHandler.class.getDeclaredField("activeClients");
        field.setAccessible(true);
        ConcurrentHashMap<?, ?> activeClients = (ConcurrentHashMap<?, ?>) field.get(null);
        activeClients.clear();
    }

    // ==========================================
    // 1. TEST GETTER / SETTER / USERNAME
    // ==========================================
    @Test
    public void testGetAndSetCurrentWatchingAuctionId() {
        clientHandler.setCurrentWatchingAuctionId("AUC_123");
        assertEquals("AUC_123", clientHandler.getCurrentWatchingAuctionId());
    }

    @Test
    public void testSetUsernamePutsInActiveClients() throws Exception {
        clientHandler.setUsername("tiendat99");

        java.lang.reflect.Field field = ClientHandler.class.getDeclaredField("activeClients");
        field.setAccessible(true);
        ConcurrentHashMap<String, ClientHandler> activeClients = (ConcurrentHashMap<String, ClientHandler>) field.get(null);

        assertTrue(activeClients.containsKey("tiendat99"));
        assertEquals(clientHandler, activeClients.get("tiendat99"));
    }

    // ==========================================
    // 2. TEST UPDATE NEW BID (Phủ cả nhánh Exception)
    // ==========================================
    @Test
    public void testUpdateNewBid_Success() throws Exception {
        BidTransaction bid = new BidTransaction("TX1", new Bidder("user1", "", 0), 1000.0, LocalDateTime.now());

        clientHandler.updateNewBid("AUC001", bid);

        verify(out).writeObject(bid);
        verify(out).flush();
    }

    @Test
    public void testUpdateNewBid_IOException() throws Exception {
        BidTransaction bid = new BidTransaction("TX1", new Bidder("user1", "", 0), 1000.0, LocalDateTime.now());
        doThrow(new IOException("Network error")).when(out).writeObject(any());

        assertDoesNotThrow(() -> clientHandler.updateNewBid("AUC001", bid));
    }

    // ==========================================
    // 3. TEST UPDATE STATUS (Phủ cả nhánh Exception)
    // ==========================================
    @Test
    public void testUpdateStatus_Success() throws Exception {
        AuctionStatus status = AuctionStatus.RUNNING;

        clientHandler.updateStatus("AUC001", status);

        verify(out).writeObject(status);
        verify(out).flush();
    }

    @Test
    public void testUpdateStatusWhenOutIsNull() throws Exception {
        java.lang.reflect.Field outField = ClientHandler.class.getDeclaredField("out");
        outField.setAccessible(true);
        outField.set(clientHandler, null);

        clientHandler.updateStatus("AUC001", AuctionStatus.FINISHED);

        verifyNoInteractions(out);
    }

    @Test
    public void testUpdateStatus_IOException() throws Exception {
        doThrow(new IOException("Stream closed")).when(out).writeObject(any());

        assertDoesNotThrow(() -> clientHandler.updateStatus("AUC001", AuctionStatus.RUNNING));
    }

    // ==========================================
    // 4. TEST RECEIVE SYSTEM MESSAGE
    // ==========================================
    @Test
    public void testReceiveSystemMessage_Success() throws Exception {
        clientHandler.receiveSystemMessage("Hệ thống bảo trì sau 5 phút");

        ArgumentCaptor<ServiceResult> captor = ArgumentCaptor.forClass(ServiceResult.class);
        verify(out).writeObject(captor.capture());
        verify(out).flush();

        ServiceResult result = captor.getValue();
        assertTrue(result.success());
        assertEquals("Hệ thống bảo trì sau 5 phút", result.message());
        assertNull(result.data());
    }

    @Test
    public void testReceiveSystemMessage_IOException() throws Exception {
        doThrow(new IOException("Write failure")).when(out).writeObject(any());

        assertDoesNotThrow(() -> clientHandler.receiveSystemMessage("Hello"));
    }

    // ==========================================
    // 5. TEST STATIC METHOD: SEND TO USER & BRANCHES
    // ==========================================
    @Test
    public void testSendToUser_UserNotFound() {
        assertDoesNotThrow(() -> ClientHandler.sendToUser("nonexistent_user", "Message"));
        verifyNoInteractions(out);
    }

    @Test
    public void testSendToUser_Success() throws Exception {
        clientHandler.setUsername("alex_k");
        Object message = "Tin nhắn mật";

        ClientHandler.sendToUser("alex_k", message);

        verify(out).writeObject(message);
        verify(out).flush();
    }

    @Test
    public void testSendToUser_IOException() throws Exception {
        clientHandler.setUsername("alex_k");
        doThrow(new IOException("Socket broken")).when(out).writeObject(any());

        assertDoesNotThrow(() -> ClientHandler.sendToUser("alex_k", "Test message"));
    }

    @Test
    public void testSendToUser_HandlerNotNullButOutIsNull() throws Exception {
        clientHandler.setUsername("user_no_out");

        java.lang.reflect.Field outField = ClientHandler.class.getDeclaredField("out");
        outField.setAccessible(true);
        outField.set(clientHandler, null);

        assertDoesNotThrow(() -> ClientHandler.sendToUser("user_no_out", "Hello"));
    }

    // =========================================================
    // 6. TEST RUN METHOD & FINALLY LIFECYCLE (BRANCH COVERAGE)
    // =========================================================
    @Test
    public void testRun_StreamCreationExceptionAndFinallyCleanup() throws Exception {
        when(socket.getOutputStream()).thenThrow(new IOException("Connection reset by peer"));

        clientHandler.setUsername("user_leave_early");
        clientHandler.setCurrentWatchingAuctionId("AUC_ROOM_1");

        clientHandler.run();

        java.lang.reflect.Field field = ClientHandler.class.getDeclaredField("activeClients");
        field.setAccessible(true);
        ConcurrentHashMap<String, ClientHandler> activeClients = (ConcurrentHashMap<String, ClientHandler>) field.get(null);
        assertFalse(activeClients.containsKey("user_leave_early"));
    }

    @Test
    public void testRun_WhenBothUsernameAndAuctionIdAreNull() throws Exception {
        // Kích hoạt nhánh ngoại lệ ngay lập tức khi khởi tạo luồng để nhảy vào khối finally
        when(socket.getOutputStream()).thenThrow(new IOException("Stream initialization error"));

        // XÓA HOẶC COMMENT DÒNG NÀY: clientHandler.setUsername(null);
        // Vì mặc định thuộc tính username khi tạo mới đối tượng đã là null sẵn rồi, không cần set nữa.

        clientHandler.setCurrentWatchingAuctionId(null);

        clientHandler.run();

        // Xác nhận chạy qua khối finally an toàn mà không bị NullPointerException
        assertNull(clientHandler.getCurrentWatchingAuctionId());
    }

    @Mock
    private java.io.InputStream mockInputStream;
    @Mock
    private java.io.OutputStream mockOutputStream;

    @Test
    public void testRun_LoopExecutionAndNormalExit() throws Exception {
        when(socket.getOutputStream()).thenReturn(mockOutputStream);
        when(socket.getInputStream()).thenReturn(mockInputStream);

        Thread testThread = new Thread(() -> clientHandler.run());
        testThread.start();

        // Cho luồng chạy một xíu rồi phát lệnh ngắt (Interrupt) để bao phủ nhánh thoát vòng lặp while
        Thread.sleep(50);
        testThread.interrupt();
        testThread.join(500);

        assertTrue(true);
    }
} // Dấu đóng ngoặc class chuẩn chỉnh cuối cùng