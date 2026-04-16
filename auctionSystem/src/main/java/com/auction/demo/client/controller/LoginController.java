package com.auction.demo.client.controller;

import com.auction.demo.client.model.LoginRequest;
import com.auction.demo.client.model.ServiceResult;
import com.auction.demo.client.service.AuthService;
import com.auction.demo.client.util.SceneNavigator;
import com.auction.shared.models.AuthUser;
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
        // Gọi Service xử lý đăng nhập
        ServiceResult<AuthUser> result = authService.login(new LoginRequest(
                usernameField.getText(),
                passwordField.getText()
        ));
        
        // Hiển thị thông báo phản hồi từ Service
        errorLabel.setText(result.message());
        errorLabel.setStyle(result.success() ? "-fx-text-fill: #059669;" : "-fx-text-fill: #dc2626;");
        
        if (result.success()) {
            try {
                // Chuyển hướng sang màn hình Danh sách đấu giá khi thành công
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
