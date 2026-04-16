package com.auction.client.controller;
import com.auction.client.util.SceneNavigator;
import com.auction.shared.models.AuctionRow;
import com.auction.client.service.AuctionCatalogService;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import java.io.IOException;
public class AuctionListController {
    private final AuctionCatalogService service = new AuctionCatalogService();
    private final ObservableList<AuctionRow> data = FXCollections.observableArrayList();
    @FXML private TextField searchField; @FXML private ComboBox<String> statusFilter;
    @FXML private TableView<AuctionRow> auctionTable; @FXML private Label resultLabel; @FXML private Button openAuctionButton;
    @FXML private TableColumn<AuctionRow, String> idColumn, itemColumn, sellerColumn, priceColumn, stepColumn, statusColumn, summaryColumn;
    @FXML public void initialize() {
        statusFilter.setItems(FXCollections.observableArrayList(service.getAvailableStatuses()));
        statusFilter.setValue("Tất cả");
        idColumn.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getAuctionId()));
        itemColumn.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getItemName()));
        sellerColumn.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getSellerName()));
        priceColumn.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getCurrentPrice()));
        stepColumn.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getStepPrice()));
        statusColumn.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getStatus()));
        summaryColumn.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getSummary()));
        auctionTable.setItems(data); reload();
    }
    @FXML private void handleSearchAction() { reload(); }
    @FXML private void handleRefreshAction() { reload(); }
    @FXML private void handleOpenAuctionAction() throws IOException {
        AuctionRow sel = auctionTable.getSelectionModel().getSelectedItem();
        if (sel != null) SceneNavigator.<AuctionRoomController>switchScene(openAuctionButton, "/views/AuctionRoom.fxml", "Phòng đấu giá", 1180, 780, c -> c.setAuctionId(sel.getAuctionId()));
    }
    @FXML private void handleLogoutAction() throws IOException { SceneNavigator.switchScene(openAuctionButton, "/views/Login.fxml", "Đăng nhập", 980, 640); }
    private void reload() { data.setAll(service.filterAuctions(searchField.getText(), statusFilter.getValue())); resultLabel.setText("Hiển thị " + data.size() + " phiên."); }
}
