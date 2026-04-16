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

public class LoginController {

    private final UserDao userDao = new UserDao();

    @FXML
    private TextField usernameField;

    @FXML
    private PasswordField passwordField;

    @FXML
    private Button loginButton;

    @FXML
    private Button goToRegisterButton;

    @FXML
    private Label errorLabel;

    // Gọi khi user click button "Đăng Nhập"
    @FXML
    private void handleLoginAction() {
        String username = usernameField.getText().trim();
        String password = passwordField.getText().trim();

        // Kiểm tra input không được để trống
        if (username.isEmpty() || password.isEmpty()) {
            ViewUtil.showMessage(errorLabel, "Vui lòng nhập đầy đủ tên đăng nhập và mật khẩu!", false);
            return;
        }

        try {
            AuthUser user = userDao.findByUsername(username);
            if (user == null) {
                ViewUtil.showMessage(errorLabel, "Tài khoản không tồn tại.", false);
                return;
            }

            if (!PasswordUtil.matches(password, user.getPasswordHash())) {
                ViewUtil.showMessage(errorLabel, "Sai mật khẩu.", false);
                return;
            }

            ViewUtil.showMessage(errorLabel, "Đăng nhập thành công. Xin chào "
                    + user.getFullName() + "!", true);
        } catch (SQLException exception) {
            ViewUtil.showMessage(errorLabel, "Không thể kết nối database. Kiểm tra MySQL.", false);
        }
    }

    @FXML
    private void handleGoToRegister() {
        try {
            ViewUtil.switchScene(goToRegisterButton, "/views/Register.fxml", "Auction System - Register",
                    600, 560);
        } catch (IOException exception) {
            ViewUtil.showMessage(errorLabel, "Không thể mở màn hình đăng ký.", false);
        }
    }
}
