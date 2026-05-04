package com.auction.client.controller;

import com.auction.client.network.SocketClient;
import com.auction.client.service.AuthService;
import com.auction.client.util.SceneNavigator;
import com.auction.shared.models.AuthUser;
import com.auction.shared.network.LoginRequest;
import com.auction.shared.network.ServiceResult;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;

import java.io.IOException;

/**
 * Controller xử lý logic đăng nhập và chuyển hướng sang màn hình đấu giá.
 */
public class LoginController {
    // Khởi tạo AuthService làm cầu nối
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
        // Lấy dữ liệu từ giao diện
        String username = usernameField.getText();
        String password = passwordField.getText();

        // Đóng gói thành Request theo đúng chuẩn
        LoginRequest request = new LoginRequest(username, password);

        // Gọi Service xử lý qua mạng
        ServiceResult<AuthUser> result = authService.login(request);

        // Hiển thị phản hồi từ Server
        errorLabel.setText(result.message());
        errorLabel.setStyle(result.success() ? "-fx-text-fill: green;" : "-fx-text-fill: red;");

        // Chuyển trang nếu thành công
        if (result.success()) {
            navigateToAuctionList();
        }
    }

    private void navigateToAuctionList() {
        try {
            SceneNavigator.switchScene(
                    loginButton,
                    "/views/AuctionList.fxml",
                    "Hệ thống đấu giá - Danh sách phiên đấu giá",
                    1200, 760
            );
        } catch (IOException e) {
            errorLabel.setText("Lỗi: Không thể mở danh sách phiên đấu giá.");
            errorLabel.setStyle("-fx-text-fill: #dc2626;");
        }
    }

    @FXML
    private void handleGoToRegister() {
        try {
            // Chuyển hướng sang màn hình Đăng ký
            SceneNavigator.switchScene(
                    loginButton,
                    "/views/Register.fxml",
                    "Hệ thống đấu giá - Đăng ký tài khoản",
                    980, 720
            );
        } catch (IOException e) {
            errorLabel.setText("Lỗi: Không thể mở màn hình đăng ký.");
        }
    }
}
