package com.auction.client.controller;

import com.auction.client.util.Scene;
import com.auction.client.util.SceneNavigator;
import com.auction.server.service.AuthService;
import com.auction.shared.models.auth.UserAccount;
import com.auction.shared.network.requests.LoginRequest;
import com.auction.client.service.SessionContext;
import com.auction.shared.network.responses.ServiceResult;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;

/**
 * Controller cho màn hình đăng nhập.
 */
public class LoginController {
  // Lấy service từ Factory thay vì khởi tạo cứng
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
    ServiceResult<UserAccount> result = authService.login(
            new LoginRequest(usernameField.getText(), passwordField.getText()));

    errorLabel.setText(result.message());
    errorLabel.setStyle(result.success() ? "-fx-text-fill: green;" : "-fx-text-fill: red;");

    if (result.success()) {
      UserAccount user = result.data();
      if (user != null) {
        SessionContext.setCurrentUser(user);
        try {
          // Dùng Enum Scene để điều hướng
          Scene target = "ADMIN".equalsIgnoreCase(user.getRole())
                  ? Scene.ADMIN_DASHBOARD
                  : Scene.AUCTION_LIST;

          SceneNavigator.switchScene(loginButton, target);
        } catch (Exception e) {
          errorLabel.setText("Lỗi chuyển trang: " + e.getMessage());
        }
      }
    }
  }

  @FXML
  private void handleGoToRegister() {
    try {
      // SỬA: Dùng Enum Scene
      SceneNavigator.switchScene(loginButton, Scene.REGISTER);
    } catch (Exception e) {
      errorLabel.setText("Lỗi hệ thống: Không thể chuyển màn hình đăng ký.");
    }
  }
}