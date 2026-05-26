package com.auction.client.controller;

import com.auction.client.service.AuctionCatalogService;
import com.auction.shared.models.AuctionRow;
import javafx.application.Platform;
import javafx.collections.ObservableList;
import javafx.scene.control.*;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.lang.reflect.Field;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

public class AuctionListControllerTest {

    @InjectMocks
    private AuctionListController controller;

    @Mock
    private AuctionCatalogService service;

    private TextField searchField;
    private ComboBox<String> statusFilter;
    private TableView<AuctionRow> auctionTable;
    private Label resultLabel;
    private Button openAuctionButton;
    private TableColumn<AuctionRow, String> idColumn;
    private TableColumn<AuctionRow, String> itemColumn;
    private TableColumn<AuctionRow, String> sellerColumn;

    // ĐÃ THÊM: Khai báo cột giả lập Người dẫn đầu
    private TableColumn<AuctionRow, String> highestBidderColumn;

    private TableColumn<AuctionRow, String> priceColumn;
    private TableColumn<AuctionRow, String> stepColumn;
    private TableColumn<AuctionRow, String> statusColumn;
    private TableColumn<AuctionRow, String> summaryColumn;

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

        Field serviceField = AuctionListController.class.getDeclaredField("service");
        serviceField.setAccessible(true);
        serviceField.set(controller, service);

        searchField = new TextField();
        statusFilter = new ComboBox<>();
        auctionTable = new TableView<>();
        resultLabel = new Label();
        openAuctionButton = new Button();
        idColumn = new TableColumn<>();
        itemColumn = new TableColumn<>();
        sellerColumn = new TableColumn<>();

        // ĐÃ THÊM: Khởi tạo cột Người dẫn đầu
        highestBidderColumn = new TableColumn<>();

        priceColumn = new TableColumn<>();
        stepColumn = new TableColumn<>();
        statusColumn = new TableColumn<>();
        summaryColumn = new TableColumn<>();

        injectField("searchField", searchField);
        injectField("statusFilter", statusFilter);
        injectField("auctionTable", auctionTable);
        injectField("resultLabel", resultLabel);
        injectField("openAuctionButton", openAuctionButton);
        injectField("idColumn", idColumn);
        injectField("itemColumn", itemColumn);
        injectField("sellerColumn", sellerColumn);

        // ĐÃ THÊM: Bơm (inject) cột Người dẫn đầu vào controller
        injectField("highestBidderColumn", highestBidderColumn);

        injectField("priceColumn", priceColumn);
        injectField("stepColumn", stepColumn);
        injectField("statusColumn", statusColumn);
        injectField("summaryColumn", summaryColumn);

        when(service.getAvailableStatuses()).thenReturn(List.of("Tất cả", "OPEN"));
    }

    private void injectField(String fieldName, Object value) throws Exception {
        Field field = AuctionListController.class.getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(controller, value);
    }

    @Test
    public void testInitialize() {
        when(service.filterAuctions(anyString(), anyString())).thenReturn(Collections.emptyList());

        controller.initialize();

        verify(service).getAvailableStatuses();
        assertEquals("Tất cả", statusFilter.getValue());
        assertEquals("Hiển thị 0 phiên.", resultLabel.getText());
    }

    @Test
    public void testReload() throws Exception {
        // ĐÃ CẬP NHẬT: Tham số mảng AuctionRow theo đúng thứ tự mới (Tách riêng TopBidder)
        AuctionRow row = new AuctionRow("AUC001", "Item", "Seller", "TopBidder", "1,000", "100", "OPEN", "Desc");
        when(service.filterAuctions(anyString(), anyString())).thenReturn(List.of(row));
        searchField.setText("test");
        statusFilter.setValue("OPEN");

        Field dataField = AuctionListController.class.getDeclaredField("data");
        dataField.setAccessible(true);
        javafx.collections.ObservableList<AuctionRow> data = (javafx.collections.ObservableList<AuctionRow>) dataField.get(controller);

        java.lang.reflect.Method method = AuctionListController.class.getDeclaredMethod("reload");
        method.setAccessible(true);
        method.invoke(controller);

        verify(service).filterAuctions("test", "OPEN");
        assertEquals(1, data.size());
        assertEquals("Hiển thị 1 phiên.", resultLabel.getText());
    }
}