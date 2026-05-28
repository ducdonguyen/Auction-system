package com.auction.client.controller;

import com.auction.client.model.AuctionRoomViewModel;
import com.auction.client.service.AuctionRoomService;
import com.auction.client.service.SessionContext;
import com.auction.shared.models.AuthUser;
import com.auction.shared.network.ServiceResult;
import javafx.application.Platform;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.lang.reflect.Field;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

public class AuctionRoomControllerTest {

    @InjectMocks
    private AuctionRoomController controller;

    @Mock
    private AuctionRoomService service;

    private Label balanceLabel;
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

        // Initialize UI components
        balanceLabel = new Label();
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

        injectField("balanceLabel", balanceLabel);
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

        // FIX 1: Use Long ID instead of String for first parameter
        AuthUser mockUser = new AuthUser(1L, "Full Name", "test", "test@gmail.com", "pass", "BIDDER", 5000.0);
        SessionContext.setCurrentUser(mockUser);
    }

    private void injectField(String fieldName, Object value) throws Exception {
        Field field = AuctionRoomController.class.getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(controller, value);
    }

    @Test
    public void testHandlePlaceBidActionInsufficientBalance() throws Exception {
        injectField("aid", "AUC-001");
        bidAmountField.setText("6000"); // Greater than 5000.0 balance

        java.lang.reflect.Method method = AuctionRoomController.class.getDeclaredMethod("handlePlaceBidAction");
        method.setAccessible(true);
        method.invoke(controller);

        assertEquals("Số dư không đủ. Vui lòng nạp thêm!", messageLabel.getText());
        verify(service, never()).placeBid(anyString(), anyString());
    }

    @Test
    public void testHandlePlaceBidActionSuccess() throws Exception {
        injectField("aid", "AUC-001");
        bidAmountField.setText("1000"); // Less than 5000.0 balance

        when(service.placeBid(eq("AUC-001"), eq("1000"))).thenReturn(new ServiceResult<>(true, "Bid placed", null));

        java.lang.reflect.Method method = AuctionRoomController.class.getDeclaredMethod("handlePlaceBidAction");
        method.setAccessible(true);
        method.invoke(controller);

        assertEquals("Bid placed", messageLabel.getText());
        verify(service).placeBid("AUC-001", "1000");
    }

    @Test
    public void testSetAuctionIdAndRender() {
        // FIX 2: Added missing parameters (itemType, extraInfo) to match record definition
        AuctionRoomViewModel vm = new AuctionRoomViewModel(
                "AUC001", "Item", "Seller", "OPEN", "100", "10", "110", "None", "Desc", "Schedule",
                java.util.Collections.emptyList(), "ELECTRONICS", "None"
        );
        when(service.getAuctionRoom("AUC001")).thenReturn(Optional.of(new ServiceResult<>(true, "", vm)));

        controller.setAuctionId("AUC001");

        assertEquals("AUC001", auctionIdLabel.getText());
        assertEquals("5,000 VNĐ", balanceLabel.getText());
    }
}
