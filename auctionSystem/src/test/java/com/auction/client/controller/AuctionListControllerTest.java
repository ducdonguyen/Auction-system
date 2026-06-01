package com.auction.client.controller;

import com.auction.client.service.AuctionCatalogService;
import com.auction.shared.models.auction.AuctionRow;
import com.auction.shared.models.auth.UserAccount;
import com.auction.shared.network.requests.CreateAuctionRequest;
import javafx.application.Platform;
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
import static org.junit.jupiter.api.Assertions.assertTrue;
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
        when(service.filterAuctions(anyString(), anyString())).thenReturn(Collections.emptyList());

        // Initialize controller to set up TableColumns and statusFilter
        controller.initialize();

        // Attach button to a Scene for SceneNavigator
        Platform.runLater(() -> {
            javafx.scene.Scene scene = new javafx.scene.Scene(new javafx.scene.layout.StackPane(openAuctionButton));
            javafx.stage.Stage stage = new javafx.stage.Stage();
            stage.setScene(scene);
        });
        Thread.sleep(200);
    }

    private void injectField(String fieldName, Object value) throws Exception {
        Field field = AuctionListController.class.getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(controller, value);
    }

    @Test
    public void testInitialize() {
        // initialize() was already called in setUp
        verify(service).getAvailableStatuses();
        assertEquals("Tất cả", statusFilter.getValue());
        assertEquals("Hiển thị 0 phiên.", resultLabel.getText());
    }

    @Test
    public void testHandleSearchAction() throws Exception {
        when(service.filterAuctions(anyString(), anyString())).thenReturn(Collections.emptyList());
        invokePrivateMethod("handleSearchAction");
        verify(service, atLeastOnce()).filterAuctions(anyString(), anyString());
    }

    @Test
    public void testHandleRefreshAction() throws Exception {
        when(service.filterAuctions(anyString(), anyString())).thenReturn(Collections.emptyList());
        invokePrivateMethod("handleRefreshAction");
        verify(service, atLeastOnce()).filterAuctions(anyString(), anyString());
    }

    @Test
    public void testHandleLogoutAction() throws Exception {
        try {
            invokePrivateMethod("handleLogoutAction");
        } catch (Exception e) {
            // Expected failure in SceneNavigator
        }
    }

    @Test
    public void testValidateAndBuildRequest() throws Exception {
        // Since record AuctionFormInputs is private, we might need to use reflection or just test the public method if possible.
        // But handleCreateAuctionAction is public. However it opens a dialog.
        // Let's try to test the validation logic by injecting mock fields and calling the private method.
        
        TextField txtName = new TextField("Product");
        TextArea txtDescription = new TextArea("Description");
        TextField txtStartingPrice = new TextField("1000");
        TextField txtPriceStep = new TextField("100");
        ComboBox<String> cbProductType = new ComboBox<>();
        cbProductType.getItems().addAll("Điện tử");
        cbProductType.setValue("Điện tử");
        TextField txtExtraInfo = new TextField("12");

        // We need to create an instance of the private record AuctionFormInputs
        Class<?> recordClass = Class.forName("com.auction.client.controller.AuctionListController$AuctionFormInputs");
        java.lang.reflect.Constructor<?> constructor = recordClass.getDeclaredConstructors()[0];
        constructor.setAccessible(true);
        Object formInputs = constructor.newInstance(txtName, txtDescription, txtStartingPrice, txtPriceStep, cbProductType, txtExtraInfo);

        java.lang.reflect.Method method = AuctionListController.class.getDeclaredMethod("validateAndBuildRequest", recordClass);
        method.setAccessible(true);
        
        // Mock SessionContext user
        UserAccount mockUser = new UserAccount(1L, "user", "Full Name", "email", "token", "BIDDER", 0.0);
        com.auction.client.service.SessionContext.setCurrentUser(mockUser);

        Object request = method.invoke(controller, formInputs);
        assertTrue(request instanceof CreateAuctionRequest);
        CreateAuctionRequest req = (CreateAuctionRequest) request;
        assertEquals("Product", req.getProductName());
        assertEquals(1000.0, req.getStartingPrice());
    }

    private void invokePrivateMethod(String methodName) throws Exception {
        java.lang.reflect.Method method = AuctionListController.class.getDeclaredMethod(methodName);
        method.setAccessible(true);
        method.invoke(controller);
    }
}