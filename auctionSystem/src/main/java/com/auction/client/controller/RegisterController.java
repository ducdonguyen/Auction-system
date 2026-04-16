package com.auction.client.controller;
import com.auction.client.model.RegistrationRequest;
import com.auction.client.model.ServiceResult;
import com.auction.client.service.AuthService;
import com.auction.client.util.SceneNavigator;
import com.auction.shared.models.AuthUser;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import java.io.IOException;
public class RegisterController {
    private final AuthService authService = new AuthService();
    @FXML private TextField fullNameField, usernameField, emailField;
    @FXML private PasswordField passwordField, confirmPasswordField;
    @FXML private Button backToLoginButton;
    @FXML private Label messageLabel;
    @FXML private void handleRegisterAction() {
        ServiceResult<AuthUser> res = authService.register(new RegistrationRequest(fullNameField.getText(), usernameField.getText(), emailField.getText(), passwordField.getText(), confirmPasswordField.getText()));
        messageLabel.setText(res.message());
        messageLabel.setStyle(res.success() ? "-fx-text-fill: green;" : "-fx-text-fill: red;");
    }
    @FXML private void handleBackToLogin() throws IOException {
        SceneNavigator.switchScene(backToLoginButton, "/views/Login.fxml", "Đăng nhập", 980, 640);
    }
}
