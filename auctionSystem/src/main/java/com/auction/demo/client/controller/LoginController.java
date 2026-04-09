package com.auction.demo.client.controller;

import com.auction.demo.client.dao.UserDao;
import com.auction.demo.client.model.AuthUser;
import com.auction.demo.client.util.PasswordUtil;
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
            showMessage("Vui lòng nhập đầy đủ tên đăng nhập và mật khẩu!", false);
            return;
        }

        try {
            AuthUser user = userDao.findByUsername(username);
            if (user == null) {
                showMessage("Tài khoản không tồn tại.", false);
                return;
            }

            if (!PasswordUtil.matches(password, user.getPasswordHash())) {
                showMessage("Sai mật khẩu.", false);
                return;
            }

            showMessage("Đăng nhập thành công. Xin chào " + user.getFullName() + "!", true);
        } catch (SQLException exception) {
            showMessage("Không thể kết nối database. Kiểm tra MySQL.", false);
        }
    }

    @FXML
    private void handleGoToRegister() {
        try {
            switchScene(goToRegisterButton, "/Register.fxml", "Auction System - Register", 600, 560);
        } catch (IOException exception) {
            showMessage("Không thể mở màn hình đăng ký.", false);
        }
    }

    private void showMessage(String message, boolean success) {
        errorLabel.setStyle(success ? "-fx-text-fill: green;" : "-fx-text-fill: red;");
        errorLabel.setText(message);
    }

    private void switchScene(Node sourceNode, String fxmlPath, String title, double width, double height) throws IOException {
        FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
        AnchorPane view = loader.load();
        Stage stage = (Stage) sourceNode.getScene().getWindow();
        stage.setScene(new Scene(view, width, height));
        stage.setTitle(title);
    }
}
