package com.auction.client.controller;

import com.auction.client.model.AuctionRoomViewModel;
import com.auction.client.network.SocketClient;
import com.auction.client.service.AuctionRoomService;
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

/**
 * Controller cho phòng đấu giá trực tuyến.
 */
public class AuctionRoomController {
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
  // THÊM MỚI 2: Ánh xạ ô hiển thị Số dư màu xanh biển cạnh ID phiên đấu giá
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
        // BẮT BUỘC: Nhờ luồng giao diện cập nhật để không bị sập app
        Platform.runLater(() -> {
          String displayName = bidTransaction.bidder().getFullName();
          if (displayName == null || displayName.trim().isEmpty()) {
            displayName = bidTransaction.bidder().getUsername();
          }
          currentPriceLabel.setText(String.format("%,.0f VNĐ", bidTransaction.bidAmount()));
          highestBidderLabel.setText(displayName);
          // Sử dụng ký hiệu %3$td (ngày), %3$tm (tháng), %3$tY (năm), %3$tH (giờ), %3$tM (phút)
          // Số 3$ nghĩa là đều lấy dữ liệu từ tham số thứ 3 truyền vào (chính là thời gian của gói tin)
          String historyLine = String.format(
                  java.util.Locale.of("vi", "VN"), // 1. Ép Locale Việt Nam vào tham số đầu tiên
                  "%1$s đặt %2$,.0f đ lúc %3$td/%3$tm/%3$tY %3$tH:%3$tM", // 2. Giữ nguyên cờ lệnh dấu phẩy ở đây
                  displayName,
                  bidTransaction.bidAmount(),
                  bidTransaction.timestamp()
          );

          bidHistoryList.getItems().add(0, historyLine);

          // Tính toán lại giá đặt tối thiểu tiếp theo hiển thị trên màn hình Client
          try {
            double currentPrice = bidTransaction.bidAmount();
            // Loại bỏ chữ " VNĐ" và dấu chấm phân tách định dạng để chuyển chuỗi bước giá về dạng số tính toán
            double stepPrice = Double.parseDouble(stepPriceLabel.getText().replaceAll("[^0-9]", ""));
            minimumBidLabel.setText(String.format("%,.0f VNĐ", currentPrice + stepPrice));
          } catch (Exception e) {
            // Bỏ qua nếu có lỗi ép kiểu định dạng chuỗi
          }
        });
      }

      @Override
      public void onStatusUpdate(AuctionStatus status) {
        // BẮT BUỘC: Nhờ luồng giao diện cập nhật
        Platform.runLater(() -> {
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
        });
      }

      // THÊM MỚI 3: Override phương thức lắng nghe biến động ví tiền thời gian thực (Fix lỗi abstract anonymous class)
      @Override
      public void onBalanceUpdate(double newBalance, double amountChanged, String reason) {
        Platform.runLater(() -> {
          // 1. Cập nhật số dư hiển thị sắc nét lên ô màu xanh biển trong phòng
          if (balanceLabel != null) {
            balanceLabel.setText(String.format("%,.0f đ", newBalance));
          }

          if (com.auction.client.service.SessionContext.getCurrentUser() != null) {
            com.auction.client.service.SessionContext.getCurrentUser().setBalance(newBalance);
          }

          // 2. In thông báo sự kiện (Hoàn tiền/Trừ tiền) trực quan lên màn hình phòng
          if (messageLabel != null && reason != null) {
            messageLabel.setStyle("-fx-text-fill: #0369a1;"); // Thiết lập màu xanh dương thông báo hệ thống
            messageLabel.setText(reason);
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

    // KIỂM TRA SỐ DƯ NGAY TẠI CLIENT: Bảo vệ luồng, tránh gửi Request vô ích lên mạng
    try {
      double bidAmount = Double.parseDouble(bidAmountStr);
      if (balanceLabel != null) {
        // Trích xuất con số từ nhãn hiển thị số dư hiện tại
        double currentBalance = Double.parseDouble(balanceLabel.getText().replaceAll("[^0-9]", ""));
        if (bidAmount > currentBalance) {
          messageLabel.setStyle("-fx-text-fill: #b91c1c;"); // Thiết lập màu đỏ cảnh báo
          messageLabel.setText("Số dư ví không đủ. Vui lòng nạp thêm tiền!");
          return;
        }
      }
    } catch (NumberFormatException e) {
      messageLabel.setStyle("-fx-text-fill: #b91c1c;");
      messageLabel.setText("Số tiền đặt thầu phải là ký tự số hợp lệ!");
      return;
    }

    // Thực hiện ném lệnh đặt cược xuống tầng Service kết nối mạng Socket
    ServiceResult<AuctionRoomViewModel> result = service.placeBid(aid, bidAmountStr);

    // Nếu thất bại (Ví dụ: sai bước giá hoặc lỗi đồng bộ Server), chuyển màu chữ thông báo thành đỏ
    if (!result.success()) {
      messageLabel.setStyle("-fx-text-fill: #b91c1c;");
    } else {
      messageLabel.setStyle("-fx-text-fill: #166534;"); // Đặt chữ màu xanh lá cây báo thành công
    }

    messageLabel.setText(result.message());
    if (result.data() != null) {
      bind(result.data());
    }
  }

  @FXML
  private void handleBackAction() throws IOException {
    SceneNavigator.switchScene(bidAmountField, "/views/AuctionList.fxml",
            "Danh sách đấu giá", 1200, 760);
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