package com.auction.client.controller;

import com.auction.client.util.Scene;
import com.auction.client.util.SceneNavigator;
import com.auction.client.network.SocketClient; // ĐÃ THÊM
import com.auction.shared.models.auth.UserAccount;
import com.auction.shared.network.requests.LoginRequest;
import com.auction.client.service.SessionContext;
import com.auction.shared.network.responses.ServiceResult;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import org.slf4j.Logger; // ĐÃ THÊM
import org.slf4j.LoggerFactory; // ĐÃ THÊM

/**
 * Controller cho màn hình đăng nhập.
 */
public class LoginController {
  private static final Logger logger = LoggerFactory.getLogger(LoginController.class);

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
    String username = usernameField.getText().trim();
    String password = passwordField.getText().trim();
    String defaultStyle = "-fx-text-fill: red;";

    // 1. CLIENT-SIDE VALIDATION: Kiểm tra rỗng tại chỗ
    if (username.isEmpty() || password.isEmpty()) {
      errorLabel.setText("Vui lòng nhập đầy đủ tài khoản và mật khẩu.");
      errorLabel.setStyle(defaultStyle);
      return;
    }

    // 2. GÓI DỮ LIỆU: Đóng gói thông tin vào LoginRequest
    LoginRequest loginRequest = new LoginRequest(username, password);

    // 3. TRUYỀN GÓI TIN ĐĂNG NHẬP QUA SOCKET LÊN SERVER (QUA NGROK)
    try {
      // Gửi gói tin lên Server của em
      SocketClient.getInstance().sendRequest(loginRequest);

      // Chờ phản hồi kết quả từ Server trả về
      Object rawResponse = SocketClient.getInstance().receiveResponse();

      // 4. XỬ LÝ KẾT QUẢ PHẢN HỒI TỪ SERVER
      if (rawResponse instanceof ServiceResult<?> result) {
        errorLabel.setText(result.message());
        errorLabel.setStyle(result.success() ? "-fx-text-fill: green;" : defaultStyle);

        if (result.success() && result.data() instanceof UserAccount user) {
          // Lưu thông tin User vừa đăng nhập thành công vào Session
          SessionContext.setCurrentUser(user);

          try {
            // Điều hướng dựa trên quyền hạn thực tế lấy từ Database của Server
            Scene target = "ADMIN".equalsIgnoreCase(user.getRole())
                    ? Scene.ADMIN_DASHBOARD
                    : Scene.AUCTION_LIST;

            SceneNavigator.switchScene(loginButton, target);
          } catch (Exception e) {
            errorLabel.setText("Lỗi chuyển trang: " + e.getMessage());
          }
        }
      } else {
        errorLabel.setText("Phản hồi từ Server không hợp lệ.");
        errorLabel.setStyle(defaultStyle);
      }
    } catch (Exception e) {
      logger.error("Lỗi kết nối khi gửi yêu cầu đăng nhập", e);
      errorLabel.setText("Lỗi kết nối mạng: Không thể kết nối tới Server.");
      errorLabel.setStyle(defaultStyle);
    }
  }

  @FXML
  private void handleGoToRegister() {
    try {
      SceneNavigator.switchScene(loginButton, Scene.REGISTER);
    } catch (Exception e) {
      errorLabel.setText("Lỗi hệ thống: Không thể chuyển màn hình đăng ký.");
    }
  }
}