package com.auction.client.controller;

import com.auction.client.model.AuctionRoomViewModel;
import com.auction.client.network.SocketClient;
import com.auction.client.service.AuctionRoomService;
import com.auction.shared.models.AuctionStatus;
import com.auction.shared.models.BidTransaction;
import com.auction.shared.models.Bidder;
import com.auction.shared.network.ServiceResult;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.lang.reflect.Field;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

public class AuctionRoomControllerTest {

    @InjectMocks
    private AuctionRoomController controller;

    @Mock
    private AuctionRoomService service;

    private Label currentPriceLabel;
    private Label highestBidderLabel;
    private Label statusLabel;
    private Label messageLabel;
    private TextField bidAmountField;
    private ListView<String> bidHistoryList;
    private Label auctionIdLabel;
    private Label itemNameLabel;
    private Label sellerLabel;
    private Label stepPriceLabel;
    private Label minimumBidLabel;
    private Label scheduleLabel;
    private Label descriptionLabel;

    // Đã bỏ @Mock và chuyển về khai báo bình thường như các Label khác
    private Label itemTypeLabel;
    private Label extraInfoLabel;

    @BeforeAll
    public static void initJavaFX() {
        try {
            Platform.startup(() -> {});
        } catch (IllegalStateException e) {
        }
    }

    @BeforeEach
    public void setUp() throws Exception {
        MockitoAnnotations.openMocks(this);

        Field serviceField = AuctionRoomController.class.getDeclaredField("service");
        serviceField.setAccessible(true);
        serviceField.set(controller, service);

        currentPriceLabel = new Label();
        highestBidderLabel = new Label();
        statusLabel = new Label();
        messageLabel = new Label();
        bidAmountField = new TextField();
        bidHistoryList = new ListView<>();
        auctionIdLabel = new Label();
        itemNameLabel = new Label();
        sellerLabel = new Label();
        stepPriceLabel = new Label();
        minimumBidLabel = new Label();
        scheduleLabel = new Label();
        descriptionLabel = new Label();

        // Khởi tạo 2 Label mới
        itemTypeLabel = new Label();
        extraInfoLabel = new Label();

        injectField("currentPriceLabel", currentPriceLabel);
        injectField("highestBidderLabel", highestBidderLabel);
        injectField("statusLabel", statusLabel);
        injectField("messageLabel", messageLabel);
        injectField("bidAmountField", bidAmountField);
        injectField("bidHistoryList", bidHistoryList);
        injectField("auctionIdLabel", auctionIdLabel);
        injectField("itemNameLabel", itemNameLabel);
        injectField("sellerLabel", sellerLabel);
        injectField("stepPriceLabel", stepPriceLabel);
        injectField("minimumBidLabel", minimumBidLabel);
        injectField("scheduleLabel", scheduleLabel);
        injectField("descriptionLabel", descriptionLabel);

        // Bơm (inject) 2 Label mới vào Controller
        injectField("itemTypeLabel", itemTypeLabel);
        injectField("extraInfoLabel", extraInfoLabel);
    }

    private void injectField(String fieldName, Object value) throws Exception {
        Field field = AuctionRoomController.class.getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(controller, value);
    }

    @Test
    public void testSetAuctionIdAndRender() {
        AuctionRoomViewModel vm = new AuctionRoomViewModel(
                "AUC001", "Item", "Seller", "OPEN", "100", "10", "110", "None", "Desc", "Schedule",
                java.util.Collections.<String>emptyList(),
                "Khác",
                "Không có thông tin"
        );
        when(service.getAuctionRoom("AUC001")).thenReturn(Optional.of(new ServiceResult<>(true, "", vm)));

        controller.setAuctionId("AUC001");

        verify(service).getAuctionRoom("AUC001");
        assertEquals("AUC001", auctionIdLabel.getText());
        assertEquals("Item", itemNameLabel.getText());

        assertEquals("Loại: Khác", itemTypeLabel.getText());
    }

    @Test
    public void testHandlePlaceBidAction() throws Exception {
        injectField("aid", "AUC001");
        bidAmountField.setText("1000");

        // SỬA Ở ĐÂY: Cho trả về null giống hệt logic thực tế của AuctionRoomService để không kích hoạt hàm bind()
        when(service.placeBid(eq("AUC001"), eq("1000"))).thenReturn(new ServiceResult<>(true, "Bid placed", null));

        java.lang.reflect.Method method = AuctionRoomController.class.getDeclaredMethod("handlePlaceBidAction");
        method.setAccessible(true);
        method.invoke(controller);

        // Lúc này chữ "Bid placed" sẽ không bị hàm bind() xóa đi nữa
        assertEquals("Bid placed", messageLabel.getText());
    }

    @Test
    public void testHandlePlaceBidActionEmpty() throws Exception {
        bidAmountField.setText("");
        invokePrivateMethod("handlePlaceBidAction");
        assertEquals("Vui lòng nhập số tiền muốn đặt!", messageLabel.getText());
    }

    @Test
    public void testHandlePlaceBidActionInvalidNumber() throws Exception {
        bidAmountField.setText("abc");
        invokePrivateMethod("handlePlaceBidAction");
        assertEquals("Số tiền đặt thầu phải là ký tự số hợp lệ!", messageLabel.getText());
    }

    @Test
    public void testHandlePlaceBidActionInsufficientBalance() throws Exception {
        Label balanceLabel = new Label("100 đ");
        injectField("balanceLabel", balanceLabel);
        bidAmountField.setText("500");
        
        invokePrivateMethod("handlePlaceBidAction");
        
        assertEquals("Số dư ví không đủ. Vui lòng nạp thêm tiền!", messageLabel.getText());
    }
@Test
public void testRealtimeCallbacks() throws Exception {
    SocketClient mockSocket = mock(SocketClient.class);
    SocketClient.setInstance(mockSocket);

    controller.initialize();

    ArgumentCaptor<SocketClient.RealtimeListener> listenerCaptor = ArgumentCaptor.forClass(SocketClient.RealtimeListener.class);
    verify(mockSocket).setRealtimeListener(listenerCaptor.capture());

    SocketClient.RealtimeListener listener = listenerCaptor.getValue();

    // Test onStatusUpdate
    Platform.runLater(() -> {
        listener.onStatusUpdate(AuctionStatus.FINISHED);
    });
    Thread.sleep(200);
    assertEquals("FINISHED", statusLabel.getText());
    assertTrue(bidAmountField.isDisable());

    // Test onBalanceUpdate
    Label balanceLabel = new Label("0 đ");
    injectField("balanceLabel", balanceLabel);
    Platform.runLater(() -> {
        listener.onBalanceUpdate(5000.0, 5000.0, "Top up");
    });
    Thread.sleep(200);
    assertEquals("5,000 đ", balanceLabel.getText());
}

    private void invokePrivateMethod(String methodName) throws Exception {
        java.lang.reflect.Method method = AuctionRoomController.class.getDeclaredMethod(methodName);
        method.setAccessible(true);
        method.invoke(controller);
    }
}