package com.auction.client.controller;

import com.auction.client.model.AuctionRoomViewModel;
import com.auction.client.network.SocketClient;
import com.auction.client.service.AuctionRoomService;
import com.auction.client.service.SessionContext;
import com.auction.client.util.SceneNavigator;
import com.auction.shared.models.AuctionStatus;
import com.auction.shared.models.BidTransaction;
import com.auction.shared.network.ServiceResult;
import java.io.IOException;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;
import javafx.scene.paint.Color;

/**
 * Controller cho phòng đấu giá trực tuyến.
 */
public class AuctionRoomController {
  private final AuctionRoomService service = new AuctionRoomService();
  private String aid;

  @FXML
  private Label balanceLabel;
  @FXML
  private Label auctionIdLabel;
  @FXML
  private Label itemNameLabel;
  @FXML
  private Label sellerLabel;
  @FXML
  private Label statusLabel;
  @FXML
  private Label currentPriceLabel;
  @FXML
  private Label stepPriceLabel;
  @FXML
  private Label minimumBidLabel;
  @FXML
  private Label highestBidderLabel;
  @FXML
  private Label scheduleLabel;
  @FXML
  private Label messageLabel;
  @FXML
  private Label descriptionLabel;
  @FXML
  private TextField bidAmountField;
  @FXML
  private ListView<String> bidHistoryList;
    @FXML
    private Label itemTypeLabel;   // THÊM MỚI
    @FXML
    private Label extraInfoLabel;   // THÊM MỚI

  /**
   * Thiết lập ID của phiên đấu giá và render giao diện.
   *
   * @param auctionId ID của phiên đấu giá.
   */
  public void setAuctionId(String auctionId) {
    this.aid = auctionId;
    render();
  }

  /**
   * Khởi tạo controller và thiết lập lắng nghe các sự kiện thời gian thực.
   */
  @FXML
  public void initialize() {
    updateBalanceUI();

    SocketClient.getInstance().setRealtimeListener(new SocketClient.RealtimeListener() {
      @Override
      public void onNewBid(BidTransaction bidTransaction) {
        Platform.runLater(() -> {
          currentPriceLabel.setText(String.format("%,.0f VNĐ", bidTransaction.bidAmount()));
          highestBidderLabel.setText(bidTransaction.bidder().getUsername());
          bidHistoryList.getItems().add(0, bidTransaction.bidder().getUsername()
                  + " vừa đặt: " + String.format("%,.0f VNĐ", bidTransaction.bidAmount()));
        });
      }

      @Override
      public void onStatusUpdate(AuctionStatus status) {
        Platform.runLater(() -> {
          statusLabel.setText(status.name());
          if (status == AuctionStatus.FINISHED || status == AuctionStatus.CANCELED) {
            bidAmountField.setDisable(true);
            if (status == AuctionStatus.CANCELED) {
              messageLabel.setText("Phiên đấu giá đã bị hủy!");
              messageLabel.setTextFill(Color.RED);
            } else {
              messageLabel.setText("Phiên đã kết thúc!");
            }
          }
        });
      }

      @Override
      public void onBalanceUpdate(double newBalance, double amountChanged, String reason) {
        Platform.runLater(() -> {
          if (SessionContext.getCurrentUser() != null) {
            SessionContext.getCurrentUser().setBalance(newBalance);
            updateBalanceUI();

            // Xử lý thông báo hoàn tiền khi phiên bị hủy (Nhiệm vụ Thành viên 4)
            if (reason != null && reason.toLowerCase().contains("hoàn tiền")) {
              String refundMsg = String.format("Phiên đã bị hủy. %,.0f VNĐ đã được hoàn về ví của bạn.", amountChanged);
              messageLabel.setText(refundMsg);
              messageLabel.setTextFill(Color.GREEN);
            }
          }
        });
      }
    });
  }

  @FXML
  private void handlePlaceBidAction() {
    String bidText = bidAmountField.getText();
    if (bidText == null || bidText.isBlank()) {
      showMessage("Vui lòng nhập số tiền!", false);
      return;
    }

    try {
      double amount = Double.parseDouble(bidText);
      double currentBalance = SessionContext.getCurrentUser().getBalance();

      // BẢO VỆ LUỒNG TIỀN: Chặn ngay tại Client (Nhiệm vụ Thành viên 4)
      if (amount > currentBalance) {
        showMessage("Số dư không đủ. Vui lòng nạp thêm!", false);
        return;
      }

      ServiceResult<AuctionRoomViewModel> result = service.placeBid(aid, bidText);
      if (result.success()) {
        showMessage(result.message(), true);
        if (result.data() != null) {
          bind(result.data());
        }
      } else {
        showMessage(result.message(), false);
      }
    } catch (NumberFormatException e) {
      showMessage("Số tiền không hợp lệ!", false);
    }
  }

  private void updateBalanceUI() {
    if (SessionContext.getCurrentUser() != null) {
      double bal = SessionContext.getCurrentUser().getBalance();
      balanceLabel.setText(String.format("%,.0f VNĐ", bal));
    }
  }

  private void showMessage(String message, boolean success) {
    messageLabel.setText(message);
    messageLabel.setTextFill(success ? Color.GREEN : Color.RED);
  }

  @FXML
  private void handleBackAction() throws IOException {
    SceneNavigator.switchScene(bidAmountField, "/views/AuctionList.fxml",
        "Danh sách đấu giá", 1200, 760);
  }

  private void render() {
    service.getAuctionRoom(aid).ifPresent(result -> bind(result.data()));
    updateBalanceUI();
  }

  private void bind(AuctionRoomViewModel viewModel) {
    auctionIdLabel.setText(viewModel.auctionId());
    itemNameLabel.setText(viewModel.itemName());
    sellerLabel.setText(viewModel.sellerName());
    statusLabel.setText(viewModel.status());
    currentPriceLabel.setText(viewModel.currentPrice());
    stepPriceLabel.setText(viewModel.stepPrice());
    minimumBidLabel.setText(viewModel.minimumBid());
    highestBidderLabel.setText(viewModel.highestBidder());
    scheduleLabel.setText(viewModel.schedule());
    descriptionLabel.setText(viewModel.description());
    itemTypeLabel.setText(viewModel.itemType());
    extraInfoLabel.setText(viewModel.extraInfo());
    bidHistoryList.setItems(FXCollections.observableArrayList(viewModel.bidHistory()));
  }
}
