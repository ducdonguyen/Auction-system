package com.auction.demo.client;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;

public class MainApp extends Application {

    @Override
    public void start(Stage primaryStage) {
        // Tạo một nhãn đơn giản để kiểm tra xem JavaFX có chạy không
        Label label = new Label("Hệ thống Đấu giá - Chào mừng bạn!");
        StackPane root = new StackPane();
        root.getChildren().add(label);

        Scene scene = new Scene(root, 400, 300);

        primaryStage.setTitle("Auction System - Client");
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    public static void main(String[] args) {
        // Lệnh kích hoạt JavaFX
        launch(args);
    }
}