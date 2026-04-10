package com.auction.client.controllers;

import com.auction.client.dao.UserDao;
import com.auction.shared.models.AuthUser;
import com.auction.client.utils.PasswordUtil;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.layout.AnchorPane;
import javafx.stage.Stage;

import java.io.IOException;
import java.sql.SQLException;

public class RegisterController {

    private final UserDao userDao = new UserDao();

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
    private Button registerButton;

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

        if (fullName.isEmpty() || username.isEmpty() || email.isEmpty() || password.isEmpty() || confirmPassword.isEmpty()) {
            showMessage("Vui lòng nhập đầy đủ thông tin.", false);
            return;
        }

        if (!email.contains("@")) {
            showMessage("Email không hợp lệ.", false);
            return;
        }

        if (!password.equals(confirmPassword)) {
            showMessage("Mật khẩu xác nhận không khớp.", false);
            return;
        }

        try {
            if (userDao.existsByUsernameOrEmail(username, email)) {
                showMessage("Tên đăng nhập hoặc email đã tồn tại.", false);
                return;
            }

            AuthUser user = new AuthUser(
                    fullName,
                    username,
                    email,
                    PasswordUtil.hashPassword(password)
            );
            userDao.register(user);
            clearForm();
            showMessage("Đăng ký thành công.", true);
        } catch (SQLException exception) {
            showMessage("Không thể lưu tài khoản vào database.", false);
        }
    }

    @FXML
    private void handleBackToLogin() {
        try {
            switchScene(backToLoginButton, "/views/Login.fxml", "Auction System - Client", 600, 400);
        } catch (IOException exception) {
            showMessage("Không thể quay lại màn hình đăng nhập.", false);
        }
    }

    private void showMessage(String message, boolean success) {
        messageLabel.setStyle(success ? "-fx-text-fill: green;" : "-fx-text-fill: red;");
        messageLabel.setText(message);
    }

    private void clearForm() {
        fullNameField.clear();
        usernameField.clear();
        emailField.clear();
        passwordField.clear();
        confirmPasswordField.clear();
    }

    private void switchScene(Node sourceNode, String fxmlPath, String title, double width, double height) throws IOException {
        FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
        AnchorPane view = loader.load();
        Stage stage = (Stage) sourceNode.getScene().getWindow();
        stage.setScene(new Scene(view, width, height));
        stage.setTitle(title);
    }
}
