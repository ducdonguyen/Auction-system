package com.auction.demo.client;
import com.auction.demo.client.service.AuthService;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.layout.AnchorPane;
import javafx.stage.Stage;
public class ClientMain extends Application {
    private final AuthService authService = new AuthService();
    @Override
    public void start(Stage primaryStage) throws Exception {
        authService.initializeDatabase();
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/Login.fxml"));
        AnchorPane view = loader.load();
        primaryStage.setTitle("Auction System - Đăng nhập");
        primaryStage.setScene(new Scene(view, 980, 640));
        primaryStage.show();
    }
    public static void main(String[] args) { launch(args); }
}
