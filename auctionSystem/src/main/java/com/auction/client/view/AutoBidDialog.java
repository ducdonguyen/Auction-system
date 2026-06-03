package com.auction.client.view;

import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;

/**
 * Dialog cài đặt Auto-Bidding.
 */
public class AutoBidDialog extends Dialog<AutoBidDialog.AutoBidConfig> {

    /**
     * Cấu hình Auto-Bidding.
     */
    public record AutoBidConfig(double maxBid, double increment) {}

    public AutoBidDialog(double currentBalance, double minIncrement) {
        setTitle("Cài đặt Auto-Bidding");
        setHeaderText("Nhập thông tin để hệ thống tự động đặt giá cho bạn.");

        ButtonType confirmButtonType = new ButtonType("Bật Auto-Bid", ButtonBar.ButtonData.OK_DONE);
        getDialogPane().getButtonTypes().addAll(confirmButtonType, ButtonType.CANCEL);

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20, 50, 10, 10));

        TextField maxBidField = new TextField();
        maxBidField.setPromptText("Ví dụ: 1,000,000");
        TextField incrementField = new TextField();
        incrementField.setPromptText("Ví dụ: 50,000");

        grid.add(new Label("Giá trần (Max Bid):"), 0, 0);
        grid.add(maxBidField, 1, 0);
        grid.add(new Label("Mức tăng tự động:"), 0, 1);
        grid.add(incrementField, 1, 1);

        getDialogPane().setContent(grid);

        // Validation logic khi nhấn nút xác nhận
        final Button confirmButton = (Button) getDialogPane().lookupButton(confirmButtonType);
        confirmButton.addEventFilter(javafx.event.ActionEvent.ACTION, event -> {
            try {
                String maxBidStr = maxBidField.getText().replaceAll("[^0-9.]", "");
                String incrementStr = incrementField.getText().replaceAll("[^0-9.]", "");
                
                double maxBid = Double.parseDouble(maxBidStr);
                double increment = Double.parseDouble(incrementStr);

                if (maxBid > currentBalance) {
                    showError("Giá trần không được lớn hơn số dư trong ví (" + String.format("%,.0f", currentBalance) + " VNĐ)");
                    event.consume();
                } else if (increment < minIncrement) {
                    showError("Mức tăng tự động không được nhỏ hơn bước giá tối thiểu (" + String.format("%,.0f", minIncrement) + " VNĐ)");
                    event.consume();
                }
            } catch (NumberFormatException e) {
                showError("Vui lòng nhập số tiền hợp lệ!");
                event.consume();
            }
        });

        setResultConverter(dialogButton -> {
            if (dialogButton == confirmButtonType) {
                try {
                    String maxBidStr = maxBidField.getText().replaceAll("[^0-9.]", "");
                    String incrementStr = incrementField.getText().replaceAll("[^0-9.]", "");
                    return new AutoBidConfig(
                            Double.parseDouble(maxBidStr),
                            Double.parseDouble(incrementStr)
                    );
                } catch (NumberFormatException e) {
                    return null;
                }
            }
            return null;
        });
    }

    private void showError(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Lỗi nhập liệu");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
