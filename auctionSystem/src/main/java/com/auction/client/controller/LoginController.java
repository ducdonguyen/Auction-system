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
      navigate("/views/AuctionList.fxml", "Hệ thống đấu giá", 1200, 760);
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
