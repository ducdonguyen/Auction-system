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
      // 1. Gọi dịch vụ đăng nhập
      ServiceResult<AuthUser> result = authService.login(new LoginRequest(usernameField.getText(), passwordField.getText()));

      // 2. Hiển thị thông báo (Thành công/Thất bại) lên UI
      errorLabel.setText(result.message());
      errorLabel.setStyle(result.success() ? "-fx-text-fill: green;" : "-fx-text-fill: red;");

      // 3. Nếu đăng nhập thành công, tiến hành kiểm tra quyền và điều hướng
      if (result.success()) {
          // Lấy thông tin user từ kết quả trả về (giả định hàm lấy dữ liệu là .data() hoặc .getData())
          AuthUser user = result.data();

          if (user != null) {
              // Lưu thông tin phiên đăng nhập vào hệ thống (nếu bạn có dùng Class Session)

              // Rẽ nhánh điều hướng dựa trên Role của User
              if ("ADMIN".equalsIgnoreCase(user.getRole())) {
                  // Điều hướng tới màn hình Admin
                  SceneNavigator.switchScene(usernameField, "/views/AdminDashboard.fxml", "Bảng điều khiển Admin", 1200, 760);
              } else {
                  // Điều hướng tới màn hình danh sách đấu giá của User
                  SceneNavigator.switchScene(usernameField, "/views/AuctionList.fxml", "Sảnh đấu giá", 1200, 760);
              }
          } else {
              errorLabel.setText("Lỗi: Không tìm thấy thông tin người dùng!");
              errorLabel.setStyle("-fx-text-fill: red;");
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
