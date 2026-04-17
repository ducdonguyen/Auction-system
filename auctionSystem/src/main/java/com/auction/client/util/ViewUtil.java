package com.auction.client.util;

import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.layout.AnchorPane;
import javafx.stage.Stage;
import java.io.IOException;

public class ViewUtil {
    private static final String SUCCESS_STYLE = "-fx-text-fill: green;";
    private static final String ERROR_STYLE = "-fx-text-fill: red;";

    /**
     * Hiển thị thông báo lên Label với màu sắc tương ứng
     */
    public static void showMessage(Label label, String message, boolean success) {
        label.setStyle(success ? SUCCESS_STYLE : ERROR_STYLE);
        label.setText(message);
    }

    /**
     * Chuyển đổi giữa các màn hình FXML
     */
    public static void switchScene(Node sourceNode, String fxmlPath, String title,
                                   double width, double height) throws IOException {
        // Sử dụng đường dẫn tuyệt đối bắt đầu bằng dấu /
        FXMLLoader loader = new FXMLLoader(ViewUtil.class.getResource(fxmlPath));
        AnchorPane view = loader.load();

        // Lấy Stage hiện tại từ node bất kỳ
        Stage stage = (Stage) sourceNode.getScene().getWindow();
        stage.setScene(new Scene(view, width, height));
        stage.setTitle(title);
        stage.centerOnScreen(); // Giúp cửa sổ luôn hiện ở giữa màn hình
    }
}