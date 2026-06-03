package com.auction.client.controller;

import com.auction.client.model.AuctionRoomViewModel;
import com.auction.client.network.SocketClient;
import com.auction.client.service.AuctionRoomService;
import com.auction.client.service.SessionContext;
import com.auction.client.util.Scene;
import com.auction.client.util.SceneNavigator;
import com.auction.shared.models.auction.AuctionStatus;
import com.auction.shared.models.auction.BidTransaction;
import com.auction.shared.network.responses.ServiceResult;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.util.Duration;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Button;
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
  private Label itemTypeLabel;
  @FXML
  private Label extraInfoLabel;
  @FXML
  private Label balanceLabel;

  @FXML
  private Label countdownTimerLabel;
  @FXML
  private Button btnPlaceBid;

  @FXML
  private LineChart<String, Number> priceChart;
  private XYChart.Series<String, Number> priceSeries;
  private javafx.animation.Timeline countdownTimeline;

  /**
   * Thiết lập ID của phiên đấu giá và render giao diện.
   */
  public void setAuctionId(String auctionId) {
    this.aid = auctionId;
    // Nạp dữ liệu trong Thread riêng để tránh treo UI
    new Thread(this::render).start();
  }

  private void startCountdown(long endTimeMillis) {
    if (countdownTimeline != null) {
      countdownTimeline.stop();
    }

    countdownTimeline = new Timeline(new KeyFrame(Duration.seconds(1), event -> {
      long now = System.currentTimeMillis();
      long diff = endTimeMillis - now;

      if (diff <= 0) {
        countdownTimerLabel.setText("ĐÃ KẾT THÚC");
        if (btnPlaceBid != null) btnPlaceBid.setDisable(true);
        if (bidAmountField != null) bidAmountField.setDisable(true);
        countdownTimeline.stop();
      } else {
        long hours = (diff / (1000 * 60 * 60));
        long minutes = (diff / (1000 * 60)) % 60;
        long seconds = (diff / 1000) % 60;
        countdownTimerLabel.setText(String.format("%02d:%02d:%02d", hours, minutes, seconds));
      }
    }));

    countdownTimeline.setCycleCount(Timeline.INDEFINITE);
    countdownTimeline.play();
  }

  /**
   * Khởi tạo controller và thiết lập lắng nghe các sự kiện thời gian thực.
   */
  @FXML
  public void initialize() {
      // Hiển thị số dư ban đầu nếu có user trong session
      if (balanceLabel != null) {
          var user = SessionContext.getCurrentUser();
          if (user != null) {
              balanceLabel.setText(String.format("%,.0f đ", user.getBalance()));
          } else {
              balanceLabel.setText("0 đ"); // tùy UX, có thể là "Chưa đăng nhập"
          }
      }
    // Khởi tạo series cho biểu đồ
    priceSeries = new XYChart.Series<>();
    priceSeries.setName("Diễn biến giá");
    if (priceChart != null) {
      priceChart.getData().add(priceSeries);
      priceChart.setAnimated(true);
    }

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
                    java.util.Locale.of("vi", "VN"),
                    "%1$s đặt %2$,.0f đ lúc %3$td/%3$tm/%3$tY %3$tH:%3$tM",
                    displayName,
                    bidTransaction.bidAmount(),
                    bidTransaction.timestamp()
            );

            bidHistoryList.getItems().add(0, historyLine);

            // Cập nhật điểm mới lên biểu đồ
            String timeLabel = String.format("%tH:%tM:%tS", bidTransaction.timestamp(), bidTransaction.timestamp(), bidTransaction.timestamp());
            priceSeries.getData().add(new XYChart.Data<>(timeLabel, bidTransaction.bidAmount()));

            double currentPrice = bidTransaction.bidAmount();
            double stepPrice = Double.parseDouble(stepPriceLabel.getText().replaceAll("[^0-9]", ""));
            minimumBidLabel.setText(String.format("%,.0f VNĐ", currentPrice + stepPrice));

          } catch (Exception e) {
            logger.error("Lỗi cập nhật giá thầu mới", e);
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
            }
          } catch (Exception e) {
            logger.error("Lỗi cập nhật trạng thái", e);
          }
        });
      }

      @Override
      public void onBalanceUpdate(double newBalance, double amountChanged, String reason) {
        Platform.runLater(() -> {
          if (balanceLabel != null) balanceLabel.setText(String.format("%,.0f đ", newBalance));
          if (SessionContext.getCurrentUser() != null) SessionContext.getCurrentUser().setBalance(newBalance);
        });
      }

      @Override
      public void onTimeUpdate(long newEndMillis) {
        Platform.runLater(() -> {
          try {
            startCountdown(newEndMillis);
          } catch (Exception e) {
            logger.error("Lỗi cập nhật thời gian phiên từ server", e);
          }
        });
      }
    });
  }

  @FXML
  private void handlePlaceBidAction() {
    String bidAmountStr = bidAmountField.getText().trim();
    if (bidAmountStr.isEmpty()) return;

    ServiceResult<AuctionRoomViewModel> result = service.placeBid(aid, bidAmountStr);
    messageLabel.setText((result.success() ? "✓ " : "✗ ") + result.message());
    if (result.success()) {
      bidAmountField.clear();
      // ✓ SỬA: Không gọi bind() hay getAuctionRoom()
      // Client sẽ nhận realtime BidTransaction event từ server → onNewBid -> cập nhật UI
    }
  }

  @FXML
  private void handleBackAction() throws IOException {
    SceneNavigator.switchScene(bidAmountField, Scene.AUCTION_LIST);
  }

  private void render() {
    try {
        service.getAuctionRoom(aid).ifPresent(result -> {
            Platform.runLater(() -> {
                try {
                    bind(result.data());
                } catch (Exception e) {
                    logger.error("Lỗi khi bind dữ liệu", e);
                    messageLabel.setText("Lỗi hiển thị dữ liệu phòng!");
                }
            });
        });
    } catch (Exception e) {
        logger.error("Lỗi khi tải phòng đấu giá", e);
        Platform.runLater(() -> messageLabel.setText("Không thể kết nối đến phòng đấu giá!"));
    }
  }

  private void bind(AuctionRoomViewModel viewModel) {
    if (viewModel == null) return;
    
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
    itemTypeLabel.setText("Loại: " + viewModel.itemType());
    extraInfoLabel.setText(viewModel.extraInfo());
    bidHistoryList.setItems(FXCollections.observableArrayList(viewModel.bidHistory()));

    // Khởi động bộ đếm ngược
    startCountdown(viewModel.endTimeMillis());

    // Nạp dữ liệu lịch sử vào biểu đồ
    if (priceSeries != null && viewModel.bidHistory() != null) {
      priceSeries.getData().clear();
      List<String> history = new ArrayList<>(viewModel.bidHistory());
      Collections.reverse(history);

      Pattern pattern = Pattern.compile(".* đặt ([\\d.,]+)\\s*(?:đ|₫|VNĐ).* lúc (.*)");
      for (String line : history) {
        Matcher matcher = pattern.matcher(line);
        if (matcher.find()) {
          try {
            String priceStr = matcher.group(1).replaceAll("[.,\\s]", "");
            double price = Double.parseDouble(priceStr);
            String fullTime = matcher.group(2);
            String timeOnly = fullTime.contains(" ") ? fullTime.substring(fullTime.indexOf(" ") + 1) : fullTime;
            priceSeries.getData().add(new XYChart.Data<>(timeOnly, price));
          } catch (Exception ignored) {}
        }
      }
    }
      // Cập nhật số dư hiển thị khi bind viewModel (đảm bảo hiển thị đúng ngay khi vào phòng)
      if (balanceLabel != null) {
          var user = SessionContext.getCurrentUser();
          if (user != null) {
              balanceLabel.setText(String.format("%,.0f đ", user.getBalance()));
          } else {
              balanceLabel.setText("0 đ");
          }
      }
  }
}
