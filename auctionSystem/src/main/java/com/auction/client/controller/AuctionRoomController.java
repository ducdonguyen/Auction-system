package com.auction.client.controller;

import com.auction.client.model.AuctionRoomViewModel;
import com.auction.client.network.SocketClient;
import com.auction.client.service.AuctionRoomService;
import com.auction.client.service.ServiceFactory;
import com.auction.client.service.SessionContext;
import com.auction.client.util.Scene;
import com.auction.client.util.SceneNavigator;
import com.auction.client.view.AutoBidDialog;
import com.auction.shared.models.auction.AuctionStatus;
import com.auction.shared.models.auction.BidTransaction;
import com.auction.shared.network.responses.ServiceResult;
import java.io.IOException;
import java.util.Locale;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleButton;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Controller cho phòng đấu giá trực tuyến.
 */
public class AuctionRoomController {
  private static final Logger logger = LoggerFactory.getLogger(AuctionRoomController.class);
  private final AuctionRoomService service = ServiceFactory.getRoomService();
  private String aid;

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
  private Label itemTypeLabel;
  @FXML
  private Label extraInfoLabel;
  @FXML
  private Label balanceLabel;
  @FXML
  private Button btnPlaceBid;

  // --- AUTO BID COMPONENTS ---
  @FXML
  private ToggleButton autoBidToggle;
  @FXML
  private Label autoBidStatusLabel;

  private AutoBidDialog.AutoBidConfig autoBidConfig;

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
    SocketClient.getInstance().setRealtimeListener(new SocketClient.RealtimeListener() {
      @Override
      public void onNewBid(BidTransaction bidTransaction) {
        Platform.runLater(() -> {
          try {
            String displayName = bidTransaction.bidder().getFullName();
            if (displayName == null || displayName.trim().isEmpty()) {
              displayName = bidTransaction.bidder().getUsername();
            }
            currentPriceLabel.setText(String.format("%,.0f VNĐ", bidTransaction.bidAmount()));
            highestBidderLabel.setText(displayName);

            String historyLine = String.format(
                    Locale.of("vi", "VN"),
                    "%1$s đặt %2$,.0f đ lúc %3$td/%3$tm/%3$tY %3$tH:%3$tM",
                    displayName,
                    bidTransaction.bidAmount(),
                    bidTransaction.timestamp()
            );

            bidHistoryList.getItems().add(0, historyLine);

            double currentPrice = bidTransaction.bidAmount();
            double stepPrice = parseCurrency(stepPriceLabel != null ? stepPriceLabel.getText() : "0");
            minimumBidLabel.setText(String.format("%,.0f VNĐ", currentPrice + stepPrice));

            // ĐÃ XÓA SẠCH ĐOẠN AUTO-BID LOGIC Ở ĐÂY VÌ SERVER SẼ TỰ LÀM VIỆC ĐÓ

          } catch (Exception e) {
            logger.error("Lỗi cập nhật giá thầu mới lên giao diện", e);
            messageLabel.setText("Lỗi cập nhật giá thầu!");
            messageLabel.setStyle("-fx-text-fill: red;");
          }
        });
      }

      @Override
      public void onStatusUpdate(AuctionStatus status) {
        Platform.runLater(() -> {
          try {
            statusLabel.setText(status.name());
            if (status == AuctionStatus.FINISHED || status == AuctionStatus.CANCELED) {
              bidAmountField.setDisable(true);
              if (autoBidToggle != null && autoBidToggle.isSelected()) {
                autoBidToggle.setSelected(false);
                handleAutoBidToggle();
              }
              if (status == AuctionStatus.CANCELED) {
                statusLabel.setStyle("-fx-background-color: #fee2e2; -fx-text-fill: #991b1b; -fx-background-radius: 20; -fx-padding: 4 12;");
                messageLabel.setText("Phiên đấu giá đã bị hủy bởi Admin!");
              } else {
                messageLabel.setText("Phiên đấu giá đã kết thúc!");
              }
            }
          } catch (Exception e) {
            logger.error("Lỗi cập nhật trạng thái phiên đấu giá", e);
          }
        });
      }

      @Override
      public void onBalanceUpdate(double newBalance, double amountChanged, String reason) {
        Platform.runLater(() -> {
          try {
            if (balanceLabel != null) {
              balanceLabel.setText(String.format("%,.0f đ", newBalance));
            }
            if (SessionContext.getCurrentUser() != null) {
              SessionContext.getCurrentUser().setBalance(newBalance);
            }
            if (messageLabel != null && reason != null) {
              messageLabel.setStyle("-fx-text-fill: #0369a1;");
              messageLabel.setText(reason);
            }
          } catch (Exception e) {
            logger.error("Lỗi cập nhật số dư ví thời gian thực", e);
          }
        });
      }
    });
  }

  @FXML
  private void handlePlaceBidAction() {
    String bidAmountStr = bidAmountField.getText().trim();
    if (bidAmountStr.isEmpty()) {
      messageLabel.setStyle("-fx-text-fill: #b91c1c;");
      messageLabel.setText("Vui lòng nhập số tiền muốn đặt!");
      return;
    }

    try {
      double bidAmount = Double.parseDouble(bidAmountStr.replaceAll("[^0-9.]", ""));
      if (balanceLabel != null) {
        double currentBalance = parseCurrency(balanceLabel.getText());
        if (bidAmount > currentBalance) {
          messageLabel.setStyle("-fx-text-fill: #b91c1c;");
          messageLabel.setText("Số dư ví không đủ. Vui lòng nạp thêm tiền!");
          return;
        }
      }
    } catch (NumberFormatException e) {
      messageLabel.setStyle("-fx-text-fill: #b91c1c;");
      messageLabel.setText("Số tiền đặt thầu phải là ký tự số hợp lệ!");
      return;
    }

    ServiceResult<AuctionRoomViewModel> result = service.placeBid(aid, bidAmountStr);
    if (!result.success()) {
      messageLabel.setStyle("-fx-text-fill: #b91c1c;");
    } else {
      messageLabel.setStyle("-fx-text-fill: #166534;");
    }
    messageLabel.setText(result.message());
    if (result.data() != null) {
      bind(result.data());
    }
  }

  @FXML
  private void handleAutoBidToggle() {
    if (autoBidToggle == null) return;

    if (autoBidToggle.isSelected()) {
      // 1. NGƯỜI DÙNG BẬT AUTO-BID
      try {
        double currentBalance = parseCurrency(balanceLabel != null ? balanceLabel.getText() : "0");
        double minIncrement = parseCurrency(stepPriceLabel != null ? stepPriceLabel.getText() : "0");

        AutoBidDialog dialog = new AutoBidDialog(currentBalance, minIncrement);
        dialog.showAndWait().ifPresentOrElse(config -> {

          // GỌI XUỐNG SERVICE BẰNG LUỒNG MẠNG (THREAD MỚI ĐỂ KHÔNG ĐƠ UI)
          new Thread(() -> {
            ServiceResult<Void> result = service.setupAutoBid(aid, config.maxBid(), config.increment());

            // HỨNG KẾT QUẢ VÀ CẬP NHẬT GIAO DIỆN
            Platform.runLater(() -> {
              if (result.success()) {
                this.autoBidConfig = config;
                autoBidToggle.setText("Auto-Bid: ON");
                autoBidToggle.setStyle("-fx-background-color: #10b981; -fx-text-fill: white; -fx-background-radius: 8;");
                if (autoBidStatusLabel != null) {
                  autoBidStatusLabel.setText(String.format("Hệ thống đang tự động đặt giá cho bạn tới mốc %,.0f VNĐ", config.maxBid()));
                  autoBidStatusLabel.setVisible(true);
                }
                messageLabel.setStyle("-fx-text-fill: #166534;");
                messageLabel.setText(result.message());
              } else {
                // Nếu Server từ chối, gạt nút Toggle về trạng thái tắt
                autoBidToggle.setSelected(false);
                resetAutoBidUI();
                messageLabel.setStyle("-fx-text-fill: #b91c1c;");
                messageLabel.setText(result.message());
              }
            });
          }).start();

        }, () -> {
          // Người dùng bấm Hủy trên Dialog cài đặt
          autoBidToggle.setSelected(false);
          resetAutoBidUI();
        });
      } catch (Exception e) {
        logger.error("Lỗi khi mở dialog Auto-Bid", e);
        autoBidToggle.setSelected(false);
        resetAutoBidUI();
      }
    } else {
      // 2. NGƯỜI DÙNG CHỦ ĐỘNG TẮT AUTO-BID
      new Thread(() -> {
        ServiceResult<Void> result = service.cancelAutoBid(aid);

        Platform.runLater(() -> {
          resetAutoBidUI(); // Luôn tắt UI dù mạng có lỗi hay không để an toàn
          if (result.success()) {
            messageLabel.setStyle("-fx-text-fill: #0369a1;");
            messageLabel.setText(result.message());
          } else {
            messageLabel.setStyle("-fx-text-fill: #b91c1c;");
            messageLabel.setText("Lỗi hủy Auto-bid: " + result.message());
          }
        });
      }).start();
    }
  }

  private void resetAutoBidUI() {
    if (autoBidToggle != null) {
      autoBidToggle.setText("Auto-Bid: OFF");
      autoBidToggle.setStyle("-fx-background-radius: 8;");
    }
    if (autoBidStatusLabel != null) {
      autoBidStatusLabel.setVisible(false);
    }
    autoBidConfig = null;
  }

  @FXML
  private void handleBackAction() throws IOException {
    SceneNavigator.switchScene(bidAmountField, Scene.AUCTION_LIST);
  }

  private void render() {
    service.getAuctionRoom(aid).ifPresent(result -> bind(result.data()));
    if (balanceLabel != null && SessionContext.getCurrentUser() != null) {
      double sessionBalance = SessionContext.getCurrentUser().getBalance();
      balanceLabel.setText(String.format("%,.0f đ", sessionBalance));
    }
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
    bidHistoryList.setItems(FXCollections.observableArrayList(viewModel.bidHistory()));

    if (itemTypeLabel != null && viewModel.itemType() != null) {
      itemTypeLabel.setText("Loại: " + viewModel.itemType());
    }
    if (extraInfoLabel != null && viewModel.extraInfo() != null) {
      extraInfoLabel.setText(viewModel.extraInfo());
    }

    if (viewModel.status() != null && (viewModel.status().equalsIgnoreCase("CANCELED") || viewModel.status().equalsIgnoreCase("ĐÃ HỦY"))) {
      statusLabel.setStyle("-fx-background-color: #fee2e2; -fx-text-fill: #991b1b; -fx-background-radius: 20; -fx-padding: 4 12;");
      bidAmountField.setDisable(true);
    }
  }

  private double parseCurrency(String text) {
    if (text == null) return 0;
    try {
      return Double.parseDouble(text.replaceAll("[^0-9]", ""));
    } catch (Exception e) {
      return 0;
    }
  }
}
