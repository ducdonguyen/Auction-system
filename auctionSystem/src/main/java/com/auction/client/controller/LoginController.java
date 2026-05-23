package com.auction.client.controller;

import com.auction.client.service.AuthService;
import com.auction.client.util.SceneNavigator;
import com.auction.shared.models.AuthUser;
import com.auction.shared.network.LoginRequest;
import com.auction.shared.network.ServiceResult;
import java.io.IOException;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;

/**
 * Controller cho màn hình đăng nhập.
 */
public class LoginController {
  private final AuthService authService = new AuthService();
  @FXML
  private TextField usernameField;
  @FXML
  private PasswordField passwordField;
  @FXML
  private Button loginButton;
  @FXML
  private Label errorLabel;

  @FXML
  private void handleLoginAction() {
      ServiceResult<AuthUser> result = authService.login(
              new LoginRequest(usernameField.getText(), passwordField.getText()));

      errorLabel.setText(result.message());
      errorLabel.setStyle(result.success() ? "-fx-text-fill: green;" : "-fx-text-fill: red;");

      if (result.success()) {
          AuthUser user = result.data(); // Hoặc result.getData() tùy code của bạn

          if (user != null) {
              // BẮT BUỘC: Bọc khối try-catch ở đây để giải quyết lỗi "unreported exception"
              try {
                  if ("ADMIN".equalsIgnoreCase(user.getRole())) {
                      // Gọi sang màn hình Admin (nhớ thêm tham số null ở cuối nếu hàm yêu cầu 6 tham số)
                      SceneNavigator.switchScene(usernameField, "/views/AdminDashboard.fxml", "Bảng điều khiển Admin", 1200, 760, null);
                  } else {
                      // Gọi sang màn hình User
                      SceneNavigator.switchScene(usernameField, "/views/AuctionList.fxml", "Sảnh đấu giá", 1200, 760, null);
                  }
              } catch (IOException e) {
                  // Nếu quá trình load file fxml lỗi, nó sẽ chạy vào đây chứ không làm sập app
                  System.err.println("Lỗi nghiêm trọng khi chuyển màn hình!");
                  e.printStackTrace();
                  errorLabel.setText("Lỗi hệ thống: Không thể mở giao diện màn hình tiếp theo.");
                  errorLabel.setStyle("-fx-text-fill: red;");
              }
          }
      }
  }

  @FXML
  private void handleGoToRegister() {
    navigate("/views/Register.fxml", "Đăng ký", 980, 720);
  }

  private void navigate(String fxmlPath, String title, int width, int height) {
    try {
      SceneNavigator.switchScene(loginButton, fxmlPath, title, width, height);
    } catch (IOException e) {
      errorLabel.setText("Lỗi điều hướng.");
    }
  }
}
