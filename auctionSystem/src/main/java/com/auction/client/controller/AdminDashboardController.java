package com.auction.client.controller;

import com.auction.client.service.AuctionCatalogService;
import com.auction.client.util.SceneNavigator;
import com.auction.shared.models.AuctionRow;
import com.auction.shared.network.ServiceResult;
import java.io.IOException;
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
import javafx.scene.paint.Color;

/**
 * Controller cho màn hình Quản lý của Admin.
 */
public class AdminDashboardController {
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
  private Label actionMessageLabel;
  @FXML
  private Button cancelAuctionButton;
  @FXML
  private Button logoutButton;

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
  private void handleCancelAuctionAction() {
    AuctionRow selected = auctionTable.getSelectionModel().getSelectedItem();
    if (selected == null) {
      showMessage("Vui lòng chọn một phiên đấu giá để hủy.", false);
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
    SceneNavigator.switchScene(logoutButton, "/views/Login.fxml", "Đăng nhập", 980, 640);
  }

  private void reload() {
    data.setAll(service.filterAuctions(searchField.getText(), statusFilter.getValue()));
    resultLabel.setText("Hiển thị " + data.size() + " phiên.");
  }

  private void showMessage(String message, boolean success) {
    actionMessageLabel.setText(message);
    actionMessageLabel.setTextFill(success ? Color.GREEN : Color.RED);
  }
}
