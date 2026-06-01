package com.auction.client.controller;

import com.auction.client.model.AuctionRoomViewModel;
import com.auction.client.network.SocketClient;
import com.auction.client.service.AuctionRoomService;
import com.auction.client.util.Scene;
import com.auction.client.util.SceneNavigator;
import com.auction.shared.models.auction.AuctionStatus;
import com.auction.shared.models.auction.BidTransaction;
import com.auction.shared.network.responses.ServiceResult;
import java.io.IOException;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Controller cho phòng đấu giá trực tuyến.
 */
public class AuctionRoomController {
  private static final Logger logger = LoggerFactory.getLogger(AuctionRoomController.class);
  private final AuctionRoomService service = new AuctionRoomService();
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
  private Label itemTypeLabel;   // THÊM MỚI
  @FXML
  private Label extraInfoLabel;   // THÊM MỚI

  @FXML
  private Label balanceLabel;

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
          try { // 2. Bọc try-catch để bảo vệ luồng UI
            String displayName = bidTransaction.bidder().getFullName();
            if (displayName == null || displayName.trim().isEmpty()) {
              displayName = bidTransaction.bidder().getUsername();
            }
            currentPriceLabel.setText(String.format("%,.0f VNĐ", bidTransaction.bidAmount()));
            highestBidderLabel.setText(displayName);

            String historyLine = String.format(
                    java.util.Locale.of("vi", "VN"),
                    "%1$s đặt %2$,.0f đ lúc %3$td/%3$tm/%3$tY %3$tH:%3$tM",
                    displayName,
                    bidTransaction.bidAmount(),
                    bidTransaction.timestamp()
            );

            bidHistoryList.getItems().add(0, historyLine);

            double currentPrice = bidTransaction.bidAmount();
            double stepPrice = Double.parseDouble(stepPriceLabel.getText().replaceAll("[^0-9]", ""));
            minimumBidLabel.setText(String.format("%,.0f VNĐ", currentPrice + stepPrice));

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

      // THÊM MỚI 3: Override phương thức lắng nghe biến động ví tiền thời gian thực (Fix lỗi abstract anonymous class)
      @Override
      public void onBalanceUpdate(double newBalance, double amountChanged, String reason) {
        Platform.runLater(() -> {
          try {
            if (balanceLabel != null) {
              balanceLabel.setText(String.format("%,.0f đ", newBalance));
            }

            if (com.auction.client.service.SessionContext.getCurrentUser() != null) {
              com.auction.client.service.SessionContext.getCurrentUser().setBalance(newBalance);
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

   /**
   * Xử lý sự kiện đặt giá - Bỏ kiểm tra balance phía client vì không an toàn.
   * FIX: Để server kiểm tra và chặn, client chỉ validate format
   */
   @FXML
   private void handlePlaceBidAction() {
     String bidAmountStr = bidAmountField.getText().trim();
     if (bidAmountStr.isEmpty()) {
       messageLabel.setStyle("-fx-text-fill: #b91c1c;");
       messageLabel.setText("Vui lòng nhập số tiền muốn đặt!");
       return;
     }

     try {
       double bidAmount = Double.parseDouble(bidAmountStr);
       if (bidAmount <= 0) {
         messageLabel.setStyle("-fx-text-fill: #b91c1c;");
         messageLabel.setText("Số tiền phải lớn hơn 0!");
         return;
       }
     } catch (NumberFormatException e) {
       messageLabel.setStyle("-fx-text-fill: #b91c1c;");
       messageLabel.setText("Số tiền đặt thầu phải là ký tự số hợp lệ!");
       return;
     }

     // Thực hiện ném lệnh đặt cược xuống tầng Service kết nối mạng Socket
     ServiceResult<AuctionRoomViewModel> result = service.placeBid(aid, bidAmountStr);

     messageLabel.setText(result.message());

     if (result.success()) {
       messageLabel.setStyle("-fx-text-fill: #166534;"); // Chữ xanh lá cây báo thành công

       // Ép giao diện trừ tiền trực tiếp để người dùng thấy số dư mới ngay lập tức
       if (com.auction.client.service.SessionContext.getCurrentUser() != null) {
         double currentMoney = com.auction.client.service.SessionContext.getCurrentUser().getBalance();
         double bidAmount = Double.parseDouble(bidAmountStr);
         double newMoney = currentMoney - bidAmount;

         // Cập nhật ví lưu trong RAM của Client
         com.auction.client.service.SessionContext.getCurrentUser().setBalance(newMoney);

         // Đẩy con số mới lên nhãn hiển thị số dư
         if (balanceLabel != null) {
           balanceLabel.setText(String.format("%,.0f đ", newMoney));
         }
       }
     } else {
       messageLabel.setStyle("-fx-text-fill: #b91c1c;"); // Chữ đỏ báo thất bại
     }
   }

  @FXML
  private void handleBackAction() throws IOException {
    SceneNavigator.switchScene(bidAmountField, Scene.AUCTION_LIST);
  }

  private void render() {
    service.getAuctionRoom(aid).ifPresent(result -> bind(result.data()));

    // ĐỒNG BỘ SỐ DƯ BAN ĐẦU: Lấy số dư tài khoản từ Session hệ thống đưa lên phòng khi vừa vào phòng
    if (balanceLabel != null && com.auction.client.service.SessionContext.getCurrentUser() != null) {
      double sessionBalance = com.auction.client.service.SessionContext.getCurrentUser().getBalance();
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
    itemTypeLabel.setText(viewModel.itemType());
    extraInfoLabel.setText(viewModel.extraInfo());
    bidHistoryList.setItems(FXCollections.observableArrayList(viewModel.bidHistory()));

    // THÊM MỚI 4: Đọc thông tin loại sản phẩm và thông tin phụ từ ViewModel ép lên giao diện Label
    if (itemTypeLabel != null && viewModel.itemType() != null) {
      itemTypeLabel.setText("Loại: " + viewModel.itemType());
    }
    if (extraInfoLabel != null && viewModel.extraInfo() != null) {
      extraInfoLabel.setText(viewModel.extraInfo());
    }

    // Cấu hình đổi màu nhãn trạng thái trực quan nếu phiên đã bị Admin hủy
    if (viewModel.status() != null && (viewModel.status().equalsIgnoreCase("CANCELED") || viewModel.status().equalsIgnoreCase("ĐÃ HỦY"))) {
      statusLabel.setStyle("-fx-background-color: #fee2e2; -fx-text-fill: #991b1b; -fx-background-radius: 20; -fx-padding: 4 12;");
      bidAmountField.setDisable(true);
    }
  }
}