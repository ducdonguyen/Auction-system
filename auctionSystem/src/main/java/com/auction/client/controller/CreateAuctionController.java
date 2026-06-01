package com.auction.client.controller;

import com.auction.client.network.SocketClient;
import com.auction.client.service.SessionContext;
import com.auction.client.util.Scene;
import com.auction.client.util.SceneNavigator;
import com.auction.shared.network.requests.CreateAuctionRequest;
import com.auction.shared.network.responses.ServiceResult;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import java.io.IOException;
import java.time.LocalDateTime;

public class CreateAuctionController {

  @FXML private TextField nameField;
  @FXML private TextArea descriptionField;
  @FXML private TextField startingPriceField;
  @FXML private TextField priceStepField;
  @FXML private ComboBox<String> productTypeCombo;
  @FXML private TextField extraInfoField;
  @FXML private Button submitButton;

  @FXML
  public void initialize() {
    productTypeCombo.getItems().addAll("Điện tử", "Tác phẩm nghệ thuật", "Xe cộ", "Thời trang", "Khác");
    productTypeCombo.setValue("Điện tử");
  }

  /**
   * Xử lý khi người dùng bấm nút "Tạo phiên" trên màn hình
   */
  @FXML
  private void handleCreateAction(ActionEvent event) {
    submitButton.setDisable(true);

    String serverType = switch (productTypeCombo.getValue()) {
      case "Điện tử" -> "ELECTRONICS";
      case "Xe cộ" -> "VEHICLE";
      case "Tác phẩm nghệ thuật", "Thời trang" -> "ART";
      default -> "OTHER";
    };

    CreateAuctionRequest request = new CreateAuctionRequest(
            nameField.getText().trim(),
            descriptionField.getText().trim(),
            Double.parseDouble(startingPriceField.getText().trim()),
            Double.parseDouble(priceStepField.getText().trim()),
            serverType,
            extraInfoField.getText().trim(),
            SessionContext.getCurrentUser() != null ? SessionContext.getCurrentUser().getUsername() : "Admin",
            LocalDateTime.now(),
            LocalDateTime.now().plusDays(3)
    );

    Task<ServiceResult<?>> task = new Task<>() {
      @Override
      protected ServiceResult<?> call() throws Exception {
        SocketClient.getInstance().sendRequest(request);
        return (ServiceResult<?>) SocketClient.getInstance().receiveResponse();
      }
    };

    task.setOnSucceeded(e -> {
      Platform.runLater(() -> {
        ServiceResult<?> result = task.getValue();
        if (result.success()) {
          // Hiện thông báo thành công
          showAlert(Alert.AlertType.INFORMATION, "Thành công", result.message());
          // Quay trở lại màn hình danh sách sau khi tạo xong
          goToCatalog();
        } else {
          submitButton.setDisable(false);
          showAlert(Alert.AlertType.ERROR, "Thất bại", result.message());
        }
      });
    });

    task.setOnFailed(e -> Platform.runLater(() -> {
      submitButton.setDisable(false);
      showAlert(Alert.AlertType.ERROR, "Lỗi kết nối", "Không thể gửi yêu cầu lên Server!");
    }));

    new Thread(task).start();
  }

  /**
   * Bấm nút "Hủy" hoặc "Quay lại"
   */
  @FXML
  private void handleCancelAction(ActionEvent event) {
    goToCatalog();
  }

  private void goToCatalog() {
    try {
      // Sử dụng bộ Navigator của bạn để quay về màn hình danh sách chính
      SceneNavigator.switchScene(submitButton, Scene.AUCTION_LIST);
    } catch (IOException e) {
      System.err.println("Không thể chuyển màn hình: " + e.getMessage());
    }
  }

  private void showAlert(Alert.AlertType type, String title, String content) {
    Alert alert = new Alert(type);
    alert.setTitle(title);
    alert.setHeaderText(null);
    alert.setContentText(content);
    alert.showAndWait();
  }
}