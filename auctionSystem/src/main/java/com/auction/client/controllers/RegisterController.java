package com.auction.client.controllers;

import com.auction.client.utils.PasswordUtil;
import com.auction.client.utils.ViewUtil;
import com.auction.server.dao.UserDao;
import com.auction.shared.models.AuthUser;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;

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

        if (fullName.isEmpty() || username.isEmpty() || email.isEmpty()
                || password.isEmpty() || confirmPassword.isEmpty()) {
            ViewUtil.showMessage(messageLabel, "Vui lòng nhập đầy đủ thông tin.", false);
            return;
        }

        if (!email.contains("@")) {
            ViewUtil.showMessage(messageLabel, "Email không hợp lệ.", false);
            return;
        }

        if (!password.equals(confirmPassword)) {
            ViewUtil.showMessage(messageLabel, "Mật khẩu xác nhận không khớp.", false);
            return;
        }

        try {
            if (userDao.existsByUsernameOrEmail(username, email)) {
                ViewUtil.showMessage(messageLabel, "Tên đăng nhập hoặc email đã tồn tại.", false);
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
            ViewUtil.showMessage(messageLabel, "Đăng ký thành công.", true);
        } catch (SQLException exception) {
            ViewUtil.showMessage(messageLabel, "Không thể lưu tài khoản vào database.", false);
        }
    }

    @FXML
    private void handleBackToLogin() {
        try {
            ViewUtil.switchScene(backToLoginButton, "/views/Login.fxml",
                    "Auction System - Client", 600, 400);
        } catch (IOException exception) {
            ViewUtil.showMessage(messageLabel, "Không thể quay lại màn hình đăng nhập.", false);
        }
    }

    private void clearForm() {
        fullNameField.clear();
        usernameField.clear();
        emailField.clear();
        passwordField.clear();
        confirmPasswordField.clear();
    }
}