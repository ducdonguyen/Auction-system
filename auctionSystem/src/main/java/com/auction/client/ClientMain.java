package com.auction.client;

import com.auction.client.dao.UserDao;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.layout.AnchorPane;
import javafx.stage.Stage;

import java.sql.SQLException;

public class ClientMain extends Application {

    @Override
    public void start(Stage primaryStage) throws Exception {
        initializeDatabase();

        // Load Login.fxml
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/Login.fxml"));
        AnchorPane loginView = loader.load();

        // Tạo scene
        Scene scene = new Scene(loginView, 600, 400);

        // Setup stage
        primaryStage.setTitle("Auction System - Client");
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }

    private void initializeDatabase() throws SQLException {
        UserDao userDao = new UserDao();
        userDao.initializeDatabase();
    }
}
