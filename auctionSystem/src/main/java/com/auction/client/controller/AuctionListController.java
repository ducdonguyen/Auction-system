package com.auction.client.controller;

import com.auction.client.service.AuctionCatalogService;
import com.auction.client.util.SceneNavigator;
import com.auction.shared.models.AuctionRow;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;

import java.io.IOException;

public class AuctionListController {
    private final AuctionCatalogService service = new AuctionCatalogService();
    private final ObservableList<AuctionRow> data = FXCollections.observableArrayList();
    @FXML
    private TextField searchField;
    @FXML
    private ComboBox<String> statusFilter;
    @FXML
    private TableView<AuctionRow> auctionTable;
    @FXML
    private Label resultLabel;
    @FXML
    private Button openAuctionButton;
    @FXML
    private TableColumn<AuctionRow, String> idColumn;
    @FXML
    private TableColumn<AuctionRow, String> itemColumn;
    @FXML
    private TableColumn<AuctionRow, String> sellerColumn;
    @FXML
    private TableColumn<AuctionRow, String> priceColumn;
    @FXML
    private TableColumn<AuctionRow, String> stepColumn;
    @FXML
    private TableColumn<AuctionRow, String> summaryColumn;
    @FXML
    private TableColumn<AuctionRow, String> statusColumn;

    @FXML
    public void initialize() {
        statusFilter.setItems(FXCollections.observableArrayList(service.getAvailableStatuses()));
        statusFilter.setValue("Tất cả");
        idColumn.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().auctionId()));
        itemColumn.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().itemName()));
        sellerColumn.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().sellerName()));
        priceColumn.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().currentPrice()));
        stepColumn.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().stepPrice()));
        statusColumn.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().status()));
        summaryColumn.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().summary()));
        auctionTable.setItems(data);
        reload();
    }

    @FXML
    private void handleSearchAction() {
        reload();
    }

    @FXML
    private void handleRefreshAction() {
        reload();
    }

    @FXML
    private void handleOpenAuctionAction() throws IOException {
        AuctionRow sel = auctionTable.getSelectionModel().getSelectedItem();
        if (sel != null) {
            SceneNavigator.<AuctionRoomController>switchScene(openAuctionButton, "/views/AuctionRoom.fxml",
                    "Phòng đấu giá", 1180, 780, c -> c.setAuctionId(sel.auctionId()));
        }
    }

    @FXML
    private void handleLogoutAction() throws IOException {
        SceneNavigator.switchScene(openAuctionButton, "/views/Login.fxml", "Đăng nhập", 980, 640);
    }

    private void reload() {
        data.setAll(service.filterAuctions(searchField.getText(), statusFilter.getValue()));
        resultLabel.setText("Hiển thị " + data.size() + " phiên.");
    }
}
