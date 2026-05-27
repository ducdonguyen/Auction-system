package com.auction.client.util;

import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.layout.AnchorPane;
import javafx.stage.Stage;

import java.io.IOException;
import java.util.function.Consumer;

public final class SceneNavigator {

    private static final String SUCCESS_STYLE = "-fx-text-fill: green;";
    private static final String ERROR_STYLE = "-fx-text-fill: red;";

    private SceneNavigator() {
        throw new UnsupportedOperationException("Utility class");
    }

    // --- PHẦN HỖ TRỢ HIỂN THỊ (Đã gộp từ ViewUtil vào) ---
    public static void showMessage(Label label, String message, boolean success) {
        label.setStyle(success ? SUCCESS_STYLE : ERROR_STYLE);
        label.setText(message);
    }

    // --- PHẦN ĐIỀU HƯỚNG MÀN HÌNH (Dùng Enum Scene) ---
    public static <T> void switchScene(Node sourceNode, com.auction.client.util.Scene scene, Consumer<T> init) throws IOException {
        FXMLLoader loader = new FXMLLoader(SceneNavigator.class.getResource(scene.fxmlPath));
        AnchorPane view = loader.load();

        if (init != null) {
            init.accept(loader.getController());
        }

        Stage stage = (Stage) sourceNode.getScene().getWindow();
        stage.setTitle(scene.title);
        stage.setScene(new Scene(view, scene.width, scene.height));
        stage.setMaximized(scene.isMaximized);

        if (!scene.isMaximized) {
            stage.setWidth(scene.width);
            stage.setHeight(scene.height);
            Platform.runLater(stage::centerOnScreen);
        }
    }

    // Overload để dùng khi không cần init controller
    public static void switchScene(Node sourceNode, com.auction.client.util.Scene scene) throws IOException {
        switchScene(sourceNode, scene, null);
    }
}
