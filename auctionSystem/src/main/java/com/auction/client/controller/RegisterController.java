package com.auction.client.controller;

import com.auction.client.network.SocketClient;
import com.auction.client.util.Scene;
import com.auction.client.util.SceneNavigator;
import com.auction.server.service.AuthService;
import com.auction.shared.models.auth.UserAccount;
import com.auction.shared.network.requests.RegistrationRequest;
import com.auction.shared.network.responses.ServiceResult;
import java.io.IOException;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Controller cho màn hình đăng ký tài khoản.
 */
public class RegisterController {
  private static final Logger logger = LoggerFactory.getLogger(RegisterController.class);
  @FXML
  private TextField fullNameField;
  @FXML
  private TextField usernameField;
  @FXML
  private TextField emailField;
  @FXML
  private PasswordField passwordField;
  @FXML
  private PasswordField confirmPasswordField;
  @FXML
  private Button backToLoginButton;
  @FXML
  private Label messageLabel;

  @FXML
  private void handleRegisterAction() {
    String fullName = fullNameField.getText().trim();
    String username = usernameField.getText().trim();
    String email = emailField.getText().trim();
    String password = passwordField.getText().trim();
    String confirmPassword = confirmPasswordField.getText().trim();
    String defaultStyle = "-fx-text-fill: red;";

    // 1. CLIENT-SIDE VALIDATION: Kiểm tra tại chỗ để bảo vệ Server
    if (fullName.isEmpty() || username.isEmpty() || email.isEmpty() || password.isEmpty()
            || confirmPassword.isEmpty()) {
      messageLabel.setText("Vui lòng nhập đầy đủ thông tin.");
      messageLabel.setStyle(defaultStyle);
      return;
    }

    if (!email.contains("@")) {
      messageLabel.setText("Email không hợp lệ.");
      messageLabel.setStyle(defaultStyle);
      return;
    }

    if (!password.equals(confirmPassword)) {
      messageLabel.setText("Mật khẩu xác nhận không khớp.");
      messageLabel.setStyle(defaultStyle);
      return;
    }

    // 2. GÓI DỮ LIỆU: Đóng gói thông tin vào Request
    RegistrationRequest request = new RegistrationRequest(fullName, username, email, password);

    // 3. ĐÃ SỬA: TRUYỀN GÓI TIN QUA SOCKET XUYÊN INTERNET (QUA NGROK)
    try {
      // Gửi gói tin RegistrationRequest lên Server của em
      SocketClient.getInstance().sendRequest(request);

      // Chờ phản hồi kết quả từ BlockingQueue của SocketClient
      Object rawResponse = SocketClient.getInstance().receiveResponse();

      // 4. HIỂN THỊ KẾT QUẢ TRẢ VỀ TỪ SERVER
      if (rawResponse instanceof ServiceResult<?> result) {
        messageLabel.setText(result.message());
        messageLabel.setStyle(result.success() ? "-fx-text-fill: green;" : "-fx-text-fill: red;");

        // Nếu Server báo đăng ký thành công trên Cloud/Database của em, xóa trắng form
        if (result.success()) {
          clearForm();
        }
      } else {
        messageLabel.setText("Phản hồi từ Server không hợp lệ.");
        messageLabel.setStyle(defaultStyle);
      }

    } catch (Exception e) {
      logger.error("Lỗi kết nối khi gửi yêu cầu đăng ký tài khoản", e);
      messageLabel.setText("Lỗi kết nối mạng: Không thể gửi yêu cầu đến Server.");
      messageLabel.setStyle(defaultStyle);
    }
  }

  @FXML
  private void handleBackToLogin() throws IOException {
    SceneNavigator.switchScene(backToLoginButton, Scene.LOGIN);
  }

  private void clearForm() {
    fullNameField.clear();
    usernameField.clear();
    emailField.clear();
    passwordField.clear();
    confirmPasswordField.clear();
  }
}

