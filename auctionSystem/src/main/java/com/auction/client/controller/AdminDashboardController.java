package com.auction.client.controller;

import com.auction.client.service.AuctionCatalogService;
import com.auction.client.util.Scene;
import com.auction.client.util.SceneNavigator;
import com.auction.shared.models.auction.AuctionRow;
import com.auction.shared.network.responses.ServiceResult;
import java.io.IOException;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TabPane;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.paint.Color;

/**
 * Controller cho màn hình Quản lý của Admin.
 */
public class AdminDashboardController {
  private final AuctionCatalogService service = new AuctionCatalogService();
  private final ObservableList<AuctionRow> data = FXCollections.observableArrayList();
  private final ObservableList<AuctionRow> pendingData = FXCollections.observableArrayList();

  @FXML
  private TextField searchField;
  @FXML
  private ComboBox<String> statusFilter;
  @FXML
  private TableView<AuctionRow> auctionTable;
  @FXML
  private TableView<AuctionRow> pendingTable;
  @FXML
  private TabPane mainTabPane;
  @FXML
  private Label resultLabel;
  @FXML
  private Label actionMessageLabel;
  @FXML
  private Button cancelAuctionButton;
  @FXML
  private Button approveButton;
  @FXML
  private Button rejectButton;
  @FXML
  private Button logoutButton;

  // CÁC CỘT CỦA BẢNG "TẤT CẢ PHIÊN ĐẤU GIÁ"
  @FXML
  private TableColumn<AuctionRow, String> idColumn;
  @FXML
  private TableColumn<AuctionRow, String> itemColumn;
  @FXML
  private TableColumn<AuctionRow, String> sellerColumn;
  @FXML
  private TableColumn<AuctionRow, String> allHighestBidderColumn;
  @FXML
  private TableColumn<AuctionRow, String> priceColumn;
  @FXML
  private TableColumn<AuctionRow, String> stepColumn;
  @FXML
  private TableColumn<AuctionRow, String> summaryColumn;
  @FXML
  private TableColumn<AuctionRow, String> statusColumn;

  // CÁC CỘT CỦA BẢNG "CHỜ DUYỆT"
  @FXML
  private TableColumn<AuctionRow, String> pIdColumn;
  @FXML
  private TableColumn<AuctionRow, String> pItemColumn;
  @FXML
  private TableColumn<AuctionRow, String> pSellerColumn;
  @FXML
  private TableColumn<AuctionRow, String> pPriceColumn;
  @FXML
  private TableColumn<AuctionRow, String> pStepColumn;
  @FXML
  private TableColumn<AuctionRow, String> pSummaryColumn;
  @FXML
  private TableColumn<AuctionRow, String> pStatusColumn;

  @FXML
  public void initialize() {
    // Lấy danh sách các trạng thái có sẵn từ service và nạp vào ComboBox, mặc định chọn "Tất cả"
    statusFilter.setItems(FXCollections.observableArrayList(service.getAvailableStatuses()));
    statusFilter.setValue("Tất cả");

    // Chỉ định bảng "Tất cả" sẽ lấy thuộc tính nào từ đối tượng AuctionRow để hiển thị lên từng cột
    idColumn.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().auctionId()));
    itemColumn.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().itemName()));
    sellerColumn.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().sellerName()));
    allHighestBidderColumn.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().highestBidder()));
    priceColumn.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().currentPrice()));
    stepColumn.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().stepPrice()));
    statusColumn.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().status()));
    summaryColumn.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().summary()));

    // Chỉ định tương tự cho bảng "Chờ duyệt" (Bảng này không cần cột Người dẫn đầu)
    pIdColumn.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().auctionId()));
    pItemColumn.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().itemName()));
    pSellerColumn.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().sellerName()));
    pPriceColumn.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().currentPrice()));
    pStepColumn.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().stepPrice()));
    pStatusColumn.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().status()));
    pSummaryColumn.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().summary()));

    auctionTable.setItems(data);
    pendingTable.setItems(pendingData);

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
  private void handleApproveAction() {
    AuctionRow selected = pendingTable.getSelectionModel().getSelectedItem();
    if (selected == null) {
      showMessage("Vui lòng chọn một phiên từ danh sách chờ duyệt.", false);
      return;
    }

    ServiceResult<Void> result = service.approveAuction(selected.auctionId());
    if (result.success()) {
      showMessage("Đã phê duyệt phiên: " + selected.auctionId(), true);
      reload();
    } else {
      showMessage(result.message(), false);
    }
  }

  @FXML
  private void handleRejectAction() {
    AuctionRow selected = pendingTable.getSelectionModel().getSelectedItem();
    if (selected == null) {
      showMessage("Vui lòng chọn một phiên từ danh sách chờ duyệt để từ chối.", false);
      return;
    }

    ServiceResult<Void> result = service.cancelAuction(selected.auctionId());
    if (result.success()) {
      showMessage("Đã từ chối phiên: " + selected.auctionId(), true);
      reload();
    } else {
      showMessage(result.message(), false);
    }
  }

  @FXML
  private void handleCancelAuctionAction() {
    AuctionRow selected = auctionTable.getSelectionModel().getSelectedItem();
    if (selected == null) {
      showMessage("Vui lòng chọn một phiên đang hoạt động để hủy.", false);
      return;
    }

    ServiceResult<Void> result = service.cancelAuction(selected.auctionId());
    if (result.success()) {
      showMessage("Đã hủy phiên đấu giá: " + selected.auctionId(), true);
      reload();
    } else {
      showMessage(result.message(), false);
    }
  }

  @FXML
  private void handleLogoutAction() throws IOException {
    SceneNavigator.switchScene(logoutButton, Scene.LOGIN);
  }

  private void reload() {
    data.setAll(service.filterAuctions(searchField.getText(), statusFilter.getValue()));
    pendingData.setAll(service.getPendingAuctions());
    resultLabel.setText("Hiển thị " + data.size() + " phiên hoạt động, " + pendingData.size() + " phiên chờ duyệt.");
  }

  private void showMessage(String message, boolean success) {
    actionMessageLabel.setText(message);
    actionMessageLabel.setTextFill(success ? Color.GREEN : Color.RED);
  }
}