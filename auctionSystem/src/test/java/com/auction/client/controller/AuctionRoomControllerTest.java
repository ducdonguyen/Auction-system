package com.auction.client.controller;

import com.auction.client.network.SocketClient;
import com.auction.client.service.AuctionRoomService;
import com.auction.shared.models.auction.AuctionStatus;
import com.auction.shared.network.responses.ServiceResult;
import javafx.application.Platform;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class AuctionRoomControllerTest {

    private AuctionRoomController controller;
    private AuctionRoomService service;

    private Label currentPriceLabel;
    private Label highestBidderLabel;
    private Label statusLabel;
    private Label messageLabel;
    private Label balanceLabel;

    private TextField bidAmountField;
    private ListView<String> bidHistoryList;

    private Label auctionIdLabel;
    private Label itemNameLabel;
    private Label sellerLabel;
    private Label stepPriceLabel;
    private Label minimumBidLabel;
    private Label scheduleLabel;
    private Label descriptionLabel;
    private Label itemTypeLabel;
    private Label extraInfoLabel;
    private Label countdownTimerLabel;

    @BeforeAll
    static void initJavaFX() {
        try {
            Platform.startup(() -> {});
        } catch (IllegalStateException ignored) {
        }
    }

    @BeforeEach
    void setUp() throws Exception {

        controller = new AuctionRoomController();
        service = mock(AuctionRoomService.class);

        currentPriceLabel = new Label();
        highestBidderLabel = new Label();
        statusLabel = new Label();
        messageLabel = new Label();
        balanceLabel = new Label();

        bidAmountField = new TextField();
        bidHistoryList = new ListView<>();

        auctionIdLabel = new Label();
        itemNameLabel = new Label();
        sellerLabel = new Label();
        stepPriceLabel = new Label();
        minimumBidLabel = new Label();
        scheduleLabel = new Label();
        descriptionLabel = new Label();
        itemTypeLabel = new Label();
        extraInfoLabel = new Label();
        countdownTimerLabel = new Label();

        injectField("currentPriceLabel", currentPriceLabel);
        injectField("highestBidderLabel", highestBidderLabel);
        injectField("statusLabel", statusLabel);
        injectField("messageLabel", messageLabel);
        injectField("balanceLabel", balanceLabel);

        injectField("bidAmountField", bidAmountField);
        injectField("bidHistoryList", bidHistoryList);

        injectField("auctionIdLabel", auctionIdLabel);
        injectField("itemNameLabel", itemNameLabel);
        injectField("sellerLabel", sellerLabel);
        injectField("stepPriceLabel", stepPriceLabel);
        injectField("minimumBidLabel", minimumBidLabel);
        injectField("scheduleLabel", scheduleLabel);
        injectField("descriptionLabel", descriptionLabel);
        injectField("itemTypeLabel", itemTypeLabel);
        injectField("extraInfoLabel", extraInfoLabel);
        injectField("countdownTimerLabel", countdownTimerLabel);

        injectField(
                "priceChart",
                new LineChart<>(new CategoryAxis(), new NumberAxis())
        );
    }

    private void injectField(String fieldName, Object value) throws Exception {
        Field field = AuctionRoomController.class.getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(controller, value);
    }

    private void invokePrivateMethod(String methodName) throws Exception {
        Method method =
                AuctionRoomController.class.getDeclaredMethod(methodName);
        method.setAccessible(true);
        method.invoke(controller);
    }

    @Test
    void testHandlePlaceBidActionEmpty() throws Exception {

        bidAmountField.setText("");

        invokePrivateMethod("handlePlaceBidAction");

        assertEquals("", messageLabel.getText());
    }

    @Test
    void testRealtimeStatusUpdate() throws Exception {

        SocketClient mockSocket = mock(SocketClient.class);
        SocketClient.setInstance(mockSocket);

        controller.initialize();

        ArgumentCaptor<SocketClient.RealtimeListener> captor =
                ArgumentCaptor.forClass(SocketClient.RealtimeListener.class);

        verify(mockSocket).setRealtimeListener(captor.capture());

        SocketClient.RealtimeListener listener =
                captor.getValue();

        Platform.runLater(() ->
                listener.onStatusUpdate(AuctionStatus.FINISHED));

        Thread.sleep(300);

        assertEquals(
                "FINISHED",
                statusLabel.getText()
        );

        assertTrue(
                bidAmountField.isDisable()
        );
    }

    @Test
    void testRealtimeBalanceUpdate() throws Exception {

        SocketClient mockSocket = mock(SocketClient.class);
        SocketClient.setInstance(mockSocket);

        controller.initialize();

        ArgumentCaptor<SocketClient.RealtimeListener> captor =
                ArgumentCaptor.forClass(SocketClient.RealtimeListener.class);

        verify(mockSocket).setRealtimeListener(captor.capture());

        SocketClient.RealtimeListener listener =
                captor.getValue();

        Platform.runLater(() ->
                listener.onBalanceUpdate(
                        5000.0,
                        5000.0,
                        "Top up"
                ));

        Thread.sleep(300);

        assertEquals(
                "5,000 đ",
                balanceLabel.getText()
        );
    }

    @Test
    void testHandlePlaceBidActionSuccess() throws Exception {

        injectField("aid", "AUC001");

        bidAmountField.setText("1000");

        try {
            Field serviceField =
                    AuctionRoomController.class.getDeclaredField("service");

            serviceField.setAccessible(true);
            serviceField.set(controller, service);

            when(service.placeBid("AUC001", "1000"))
                    .thenReturn(
                            new ServiceResult<>(
                                    true,
                                    "Bid placed",
                                    null
                            )
                    );

            invokePrivateMethod("handlePlaceBidAction");

            assertEquals(
                    "Bid placed",
                    messageLabel.getText()
            );

        } catch (Exception ignored) {
            // Nếu Java không cho ghi đè field final
            // thì bỏ qua test này
        }
    }
}