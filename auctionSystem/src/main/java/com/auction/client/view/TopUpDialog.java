package com.auction.client.view; // Điều chỉnh package cho đúng dự án của bạn

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;

import java.util.function.Consumer;

public class TopUpDialog extends Dialog<Double> {

    public TopUpDialog(Consumer<Double> onTopUpSelected) {
        setTitle("Nạp Tiền Vào Tài Khoản");
        setHeaderText("Chọn hoặc nhập số tiền bạn muốn nạp");

        // Set Button Types (Nút Xác nhận và Hủy)
        ButtonType topUpButtonType = new ButtonType("Nạp tiền", ButtonBar.ButtonData.OK_DONE);
        getDialogPane().getButtonTypes().addAll(topUpButtonType, ButtonType.CANCEL);

        // Khởi tạo layout chính
        VBox root = new VBox(15);
        root.setPadding(new Insets(20));
        root.setAlignment(Pos.CENTER);

        // Ô nhập số tiền tùy chỉnh
        TextField amountField = new TextField();
        amountField.setPromptText("Nhập số tiền khác (đ)...");
        amountField.setStyle("-fx-font-size: 14px;");

        // Lưới chứa các nút mốc nạp nhanh
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setAlignment(Pos.CENTER);

        double[] quickAmounts = {100000, 200000, 500000, 1000000, 2000000, 5000000};
        String[] labels = {"100k", "200k", "500k", "1 Triệu", "2 Triệu", "5 Triệu"};

        int row = 0, col = 0;
        for (int i = 0; i < quickAmounts.length; i++) {
            double amount = quickAmounts[i];
            Button btn = new Button(labels[i]);
            btn.setPrefSize(100, 40);
            btn.setStyle("-fx-background-color: #e0e0e0; -fx-font-weight: bold; -fx-cursor: hand;");

            // Hiệu ứng hover cho nút mốc nạp nhanh
            btn.setOnMouseEntered(e -> btn.setStyle("-fx-background-color: #bdbdbd; -fx-font-weight: bold; -fx-cursor: hand;"));
            btn.setOnMouseExited(e -> btn.setStyle("-fx-background-color: #e0e0e0; -fx-font-weight: bold;"));

            // Khi click nút nhanh -> điền thẳng số tiền vào TextField
            btn.setOnAction(e -> amountField.setText(String.format("%.0f", amount)));

            grid.add(btn, col, row);
            col++;
            if (col > 2) { // 3 cột một hàng
                col = 0;
                row++;
            }
        }

        root.getChildren().addAll(new Label("Mốc nạp nhanh:"), grid, new Label("Số tiền tùy chỉnh:"), amountField);
        getDialogPane().setContent(root);

        // Ép kiểu dữ liệu trả về khi bấm nút "Nạp tiền"
        setResultConverter(dialogButton -> {
            if (dialogButton == topUpButtonType) {
                try {
                    return Double.parseDouble(amountField.getText());
                } catch (NumberFormatException e) {
                    return 0.0;
                }
            }
            return null;
        });
    }
}