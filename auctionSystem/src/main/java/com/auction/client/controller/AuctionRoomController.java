package com.auction.client.controller;

import com.auction.client.model.AuctionRoomViewModel;
import com.auction.client.network.SocketClient;
import com.auction.client.service.AuctionRoomService;
import com.auction.client.util.SceneNavigator;
import com.auction.shared.models.AuctionStatus;
import com.auction.shared.models.BidTransaction;
import com.auction.shared.network.ServiceResult;
import java.io.IOException;
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
  private Label itemTypeLabel;
  @FXML
  private Label extraInfoLabel;
  @FXML
  private TextField bidAmountField;
  @FXML
  private javafx.scene.control.Button btnPlaceBid;
  @FXML
  private ListView<String> bidHistoryList;

  /**
   * Thiết lập ID của phiên đấu giá và render giao diện.
   *
   * @param auctionId ID của phiên đấu giá.
   */
  public void setAuctionId(String auctionId) {
    this.aid = auctionId;
    render();
  }

  private final java.text.NumberFormat cf = java.text.NumberFormat.getCurrencyInstance(java.util.Locale.of("vi", "VN"));
  private double stepPriceValue = 0;

  /**
   * Khởi tạo controller và thiết lập lắng nghe các sự kiện thời gian thực.
   */
  @FXML
  public void initialize() {
    SocketClient.getInstance().setRealtimeListener(new SocketClient.RealtimeListener() {
      @Override
      public void onNewBid(BidTransaction bidTransaction) {
        // BẮT BUỘC: Nhờ luồng giao diện cập nhật để không bị sập app
        javafx.application.Platform.runLater(() -> {
          double newPrice = bidTransaction.bidAmount();
          // TỰ ĐỘNG TÍNH TOÁN: Giá tối thiểu tiếp theo = Giá vừa đặt + Bước giá của phòng
          double nextMinBid = newPrice + stepPriceValue;

          // Cập nhật các nhãn bằng bộ định dạng tiền tệ cf để không bị dính chữ E
          currentPriceLabel.setText(cf.format(newPrice));
          minimumBidLabel.setText(cf.format(nextMinBid));
          highestBidderLabel.setText(bidTransaction.bidder().getUsername());

          // Định dạng thời gian và đưa dòng lịch sử mới nhất lên đầu danh sách (Index 0)
          String timeStr = java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm").format(bidTransaction.timestamp());
          bidHistoryList.getItems().add(0, bidTransaction.bidder().getUsername()
                  + " đặt " + cf.format(newPrice)
                  + " lúc " + timeStr);
        });
      }

      @Override
      public void onStatusUpdate(AuctionStatus status) {
        // BẮT BUỘC: Nhờ luồng giao diện cập nhật
        javafx.application.Platform.runLater(() -> {
          statusLabel.setText(status.name());

          boolean isBiddable = (status == AuctionStatus.OPEN || status == AuctionStatus.RUNNING);
          bidAmountField.setDisable(!isBiddable);
          if (btnPlaceBid != null) btnPlaceBid.setDisable(!isBiddable);

          if (!isBiddable) {
            messageLabel.setText("Phiên đã kết thúc hoặc bị hủy!");
            messageLabel.setStyle("-fx-text-fill: red;");
          }
        });
      }
    });
  }

  @FXML
  private void handlePlaceBidAction() {
    // Gọi tầng Service xử lý và lấy kết quả kết nối mạng
    ServiceResult<AuctionRoomViewModel> result = service.placeBid(aid, bidAmountField.getText());

    // Hiển thị thông điệp động (Server trả về lỗi gì thì hiện lỗi đó)
    messageLabel.setText(result.message());

    if (result.success()) {
      messageLabel.setStyle("-fx-text-fill: #166534;");
      bidAmountField.clear(); // Xóa sạch ô nhập để sẵn sàng cho lần đặt sau
    } else {
      messageLabel.setStyle("-fx-text-fill: #dc2626;");
    }

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
  }

  private void bind(AuctionRoomViewModel viewModel) {
    auctionIdLabel.setText(viewModel.auctionId());
    itemNameLabel.setText(viewModel.itemName());
    sellerLabel.setText("Người bán: " + viewModel.sellerName());
    statusLabel.setText(viewModel.status());
    currentPriceLabel.setText(viewModel.currentPrice());
    stepPriceLabel.setText(viewModel.stepPrice());
    minimumBidLabel.setText(viewModel.minimumBid());
    highestBidderLabel.setText(viewModel.highestBidder());
    scheduleLabel.setText(viewModel.schedule());
    descriptionLabel.setText(viewModel.description());
    bidHistoryList.setItems(FXCollections.observableArrayList(viewModel.bidHistory()));
    itemTypeLabel.setText(viewModel.itemType());
    extraInfoLabel.setText(viewModel.extraInfo());

    try {
      String cleanStep = viewModel.stepPrice().replaceAll("\\D", "");
      this.stepPriceValue = Double.parseDouble(cleanStep);
    } catch (Exception e) {
      this.stepPriceValue = 0;
    }

    boolean isBiddable = viewModel.status().equals("OPEN") || viewModel.status().equals("RUNNING");
    bidAmountField.setDisable(!isBiddable); // Khóa ô nhập
    if (btnPlaceBid != null) btnPlaceBid.setDisable(!isBiddable); // Khóa nút bấm

    if (!isBiddable) {
      messageLabel.setText("Phiên đấu giá không trong trạng thái mở.");
      messageLabel.setStyle("-fx-text-fill: red;");
    } else {
      messageLabel.setText(""); // Xóa thông báo lỗi nếu hợp lệ
      messageLabel.setStyle("-fx-text-fill: black;");
    }
  }
}
