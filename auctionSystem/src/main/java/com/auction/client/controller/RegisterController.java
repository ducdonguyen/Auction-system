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
        ServiceResult<AuthUser> result = authService.register(new RegistrationRequest(
                fullNameField.getText(),
                usernameField.getText(),
                emailField.getText(),
                passwordField.getText(),
                confirmPasswordField.getText()
        ));
        messageLabel.setText(result.message());
        messageLabel.setStyle(result.success() ? "-fx-text-fill: green;" : "-fx-text-fill: red;");
    }

    @FXML
    private void handleBackToLogin() throws IOException {
        SceneNavigator.switchScene(backToLoginButton, "/views/Login.fxml", "Đăng nhập", 980, 640);
    }
}
