package com.auction.client;

import com.auction.client.network.SocketClient;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.layout.AnchorPane;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

public class ClientMain extends Application {
    private static final Logger logger = LoggerFactory.getLogger(ClientMain.class);

    @Override
    public void start(Stage primaryStage) throws Exception {
        try {
            SocketClient.getInstance().connect();
        } catch (IOException e) {
            logger.error("Không thể kết nối đến Server. Vui lòng bật Server trước!", e);
        }

        // Load Login.fxml
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/Login.fxml"));
        AnchorPane loginView = loader.load();

        Scene scene = new Scene(loginView, 600, 400);

        primaryStage.setTitle("Auction System - Client");
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    @Override
    public void stop() {
        SocketClient.getInstance().disconnect();
    }

    public static void main(String[] args) {
        launch(args);
    }

}
