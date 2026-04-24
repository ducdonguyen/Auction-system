package com.auction.client.controller;

import com.auction.client.service.AuthService;
import com.auction.client.util.SceneNavigator;
import com.auction.shared.models.AuthUser;
import com.auction.shared.network.RegistrationRequest;
import com.auction.shared.network.ServiceResult;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;

import java.io.IOException;

public class RegisterController {
    private final AuthService authService = new AuthService();
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

        // 1. CLIENT-SIDE VALIDATION: Kiểm tra tại chỗ để bảo vệ Server
        if (fullName.isEmpty() || username.isEmpty() || email.isEmpty() || password.isEmpty() ||
                confirmPassword.isEmpty()) {
            messageLabel.setText("Vui lòng nhập đầy đủ thông tin.");
            messageLabel.setStyle("-fx-text-fill: red;");
            return; // Dừng lại luôn, không gửi lên Server
        }

        if (!email.contains("@")) {
            messageLabel.setText("Email không hợp lệ.");
            messageLabel.setStyle("-fx-text-fill: red;");
            return;
        }

        if (!password.equals(confirmPassword)) {
            messageLabel.setText("Mật khẩu xác nhận không khớp.");
            messageLabel.setStyle("-fx-text-fill: red;");
            return;
        }

        // 2. GÓI DỮ LIỆU: Chỉ truyền 4 trường cần thiết lên Server
        RegistrationRequest request = new RegistrationRequest(fullName, username, email, password);

        // 3. GỌI SERVICE XỬ LÝ MẠNG
        ServiceResult<AuthUser> result = authService.register(request);

        // 4. HIỂN THỊ KẾT QUẢ
        messageLabel.setText(result.message());
        messageLabel.setStyle(result.success() ? "-fx-text-fill: green;" : "-fx-text-fill: red;");

        // Nếu đăng ký thành công, xóa trắng form cho đẹp
        if (result.success()) {
            clearForm();
        }
    }

    @FXML
    private void handleBackToLogin() throws IOException {
        SceneNavigator.switchScene(backToLoginButton, "/views/Login.fxml", "Đăng nhập", 980, 640);
    }

    private void clearForm() {
        fullNameField.clear();
        usernameField.clear();
        emailField.clear();
        passwordField.clear();
        confirmPasswordField.clear();
    }
}

