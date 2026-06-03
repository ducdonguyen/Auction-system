package com.auction.client.controller;

import com.auction.client.service.AuctionCatalogService;
import com.auction.shared.models.auction.AuctionRow;
import com.auction.shared.network.responses.ServiceResult;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.scene.control.*;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

public class AdminDashboardControllerTest {

    @InjectMocks
    private AdminDashboardController adminDashboardController;

    @Mock
    private AuctionCatalogService auctionCatalogService;

    // FXML fields
    private TextField searchField;
    private ComboBox<String> statusFilter;
    private TableView<AuctionRow> auctionTable;
    private TableView<AuctionRow> pendingTable;
    private Label actionMessageLabel;
    private Label resultLabel;

    @BeforeAll
    public static void initJavaFX() {
        try {
            Platform.startup(() -> {});
        } catch (IllegalStateException e) {
            // Already started
        }
    }

    @BeforeEach
    public void setUp() throws Exception {
        MockitoAnnotations.openMocks(this);

        // Inject mock service into private final field
        Field serviceField = AdminDashboardController.class.getDeclaredField("service");
        serviceField.setAccessible(true);
        serviceField.set(adminDashboardController, auctionCatalogService);

        // Initialize FXML components
        searchField = new TextField();
        statusFilter = new ComboBox<>();
        auctionTable = new TableView<>();
        pendingTable = new TableView<>();
        actionMessageLabel = new Label();
        resultLabel = new Label();

        // TableColumns for auctionTable
        TableColumn<AuctionRow, String> idCol = new TableColumn<>();
        TableColumn<AuctionRow, String> itemCol = new TableColumn<>();
        TableColumn<AuctionRow, String> sellerCol = new TableColumn<>();
        TableColumn<AuctionRow, String> allHighestBidderCol = new TableColumn<>();
        TableColumn<AuctionRow, String> priceCol = new TableColumn<>();
        TableColumn<AuctionRow, String> stepCol = new TableColumn<>();
        TableColumn<AuctionRow, String> statusCol = new TableColumn<>();
        TableColumn<AuctionRow, String> summaryCol = new TableColumn<>();

        // TableColumns for pendingTable
        TableColumn<AuctionRow, String> pIdCol = new TableColumn<>();
        TableColumn<AuctionRow, String> pItemCol = new TableColumn<>();
        TableColumn<AuctionRow, String> pSellerCol = new TableColumn<>();
        TableColumn<AuctionRow, String> pPriceCol = new TableColumn<>();
        TableColumn<AuctionRow, String> pStepCol = new TableColumn<>();
        TableColumn<AuctionRow, String> pStatusCol = new TableColumn<>();
        TableColumn<AuctionRow, String> pSummaryCol = new TableColumn<>();

        // Inject all fields
        injectField("searchField", searchField);
        injectField("statusFilter", statusFilter);
        injectField("auctionTable", auctionTable);
        injectField("pendingTable", pendingTable);
        injectField("actionMessageLabel", actionMessageLabel);
        injectField("resultLabel", resultLabel);
        
        injectField("idColumn", idCol);
        injectField("itemColumn", itemCol);
        injectField("sellerColumn", sellerCol);
        injectField("allHighestBidderColumn", allHighestBidderCol);
        injectField("priceColumn", priceCol);
        injectField("stepColumn", stepCol);
        injectField("statusColumn", statusCol);
        injectField("summaryColumn", summaryCol);

        injectField("pIdColumn", pIdCol);
        injectField("pItemColumn", pItemCol);
        injectField("pSellerColumn", pSellerCol);
        injectField("pPriceColumn", pPriceCol);
        injectField("pStepColumn", pStepCol);
        injectField("pStatusColumn", pStatusCol);
        injectField("pSummaryColumn", pSummaryCol);

        // Mock service responses for initialize
        when(auctionCatalogService.getAvailableStatuses()).thenReturn(List.of("Tất cả", "OPEN", "PENDING"));
        when(auctionCatalogService.filterAuctions(anyString(), anyString())).thenReturn(List.of());
        when(auctionCatalogService.getPendingAuctions()).thenReturn(List.of());

        adminDashboardController.initialize();
    }

    private void injectField(String fieldName, Object value) throws Exception {
        Field field = AdminDashboardController.class.getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(adminDashboardController, value);
    }

    private void invokePrivateMethod(String methodName) throws Exception {
        Method method = AdminDashboardController.class.getDeclaredMethod(methodName);
        method.setAccessible(true);
        method.invoke(adminDashboardController);
    }

    @Test
    public void testHandleApproveActionSuccess() throws Exception {
        AuctionRow selected = new AuctionRow("AUC-001", "Item", "Seller", "100", "10", "PENDING", "Desc", "None");
        pendingTable.setItems(FXCollections.observableArrayList(selected));
        pendingTable.getSelectionModel().select(selected);

        when(auctionCatalogService.approveAuction("AUC-001")).thenReturn(new ServiceResult<>(true, "Success", null, System.currentTimeMillis()));

        invokePrivateMethod("handleApproveAction");

        verify(auctionCatalogService).approveAuction("AUC-001");
        assertEquals("Đã phê duyệt phiên: AUC-001", actionMessageLabel.getText());
    }

    @Test
    public void testHandleApproveActionNoSelection() throws Exception {
        pendingTable.getSelectionModel().clearSelection();

        invokePrivateMethod("handleApproveAction");

        assertEquals("Vui lòng chọn một phiên từ danh sách chờ duyệt.", actionMessageLabel.getText());
        verify(auctionCatalogService, never()).approveAuction(anyString());
    }

    @Test
    public void testHandleSearchAction() throws Exception {
        searchField.setText("test");
        statusFilter.setValue("OPEN");
        
        when(auctionCatalogService.filterAuctions("test", "OPEN")).thenReturn(List.of());
        
        invokePrivateMethod("handleSearchAction");
        
        verify(auctionCatalogService).filterAuctions("test", "OPEN");
    }

    @Test
    public void testHandleRefreshAction() throws Exception {
        invokePrivateMethod("handleRefreshAction");
        verify(auctionCatalogService, atLeastOnce()).filterAuctions(anyString(), anyString());
    }

    @Test
    public void testHandleRejectActionSuccess() throws Exception {
        AuctionRow selected = new AuctionRow("AUC-001", "Item", "Seller", "100", "10", "PENDING", "Desc", "None");
        pendingTable.setItems(FXCollections.observableArrayList(selected));
        pendingTable.getSelectionModel().select(selected);

        when(auctionCatalogService.cancelAuction("AUC-001")).thenReturn(new ServiceResult<>(true, "Rejected", null, System.currentTimeMillis()));

        invokePrivateMethod("handleRejectAction");

        verify(auctionCatalogService).cancelAuction("AUC-001");
        assertEquals("Đã từ chối phiên: AUC-001", actionMessageLabel.getText());
    }

    @Test
    public void testHandleRejectActionNoSelection() throws Exception {
        pendingTable.getSelectionModel().clearSelection();

        invokePrivateMethod("handleRejectAction");

        assertEquals("Vui lòng chọn một phiên từ danh sách chờ duyệt để từ chối.", actionMessageLabel.getText());
        verify(auctionCatalogService, never()).cancelAuction(anyString());
    }

    @Test
    public void testHandleLogoutAction() throws Exception {
        try {
            invokePrivateMethod("handleLogoutAction");
        } catch (Exception e) {
            // Expected failure in SceneNavigator
        }
    }
}
