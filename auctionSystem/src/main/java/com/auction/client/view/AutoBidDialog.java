package com.auction.client.view;

import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;

import java.text.NumberFormat;
import java.util.Locale;

/**
 * Hộp thoại (Popup) cho phép người dùng cấu hình các thông số Đấu giá tự động (Auto-Bidding).
 */
public class AutoBidDialog extends Dialog<AutoBidDialog.AutoBidConfig> {

    // Record (hoặc class) chứa 2 thông số trả về khi người dùng bấm Xác nhận
    public record AutoBidConfig(double maxBid, double increment) {}

    private TextField maxBidField;
    private TextField incrementField;
    private final NumberFormat cf = NumberFormat.getCurrencyInstance(Locale.of("vi", "VN"));

    public AutoBidDialog(double currentBalance, double minIncrement) {
        setTitle("Cài đặt Auto-Bid");
        setHeaderText("Hệ thống sẽ tự động đặt giá thay bạn.\nSố dư ví hiện tại: " + cf.format(currentBalance));

        // 1. Khai báo các nút bấm
        ButtonType confirmButtonType = new ButtonType("Bật Auto-Bid", ButtonBar.ButtonData.OK_DONE);
        getDialogPane().getButtonTypes().addAll(confirmButtonType, ButtonType.CANCEL);

        // 2. Thiết kế Layout dạng lưới (Grid)
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(15);
        grid.setPadding(new Insets(20, 30, 10, 30));

        maxBidField = new TextField();
        maxBidField.setPromptText("Ví dụ: 5000000");

        incrementField = new TextField();
        incrementField.setPromptText("Tối thiểu: " + cf.format(minIncrement));
        incrementField.setText(String.valueOf((long) minIncrement)); // Gợi ý sẵn bước giá tối thiểu

        grid.add(new Label("Giá trần (Max Bid):"), 0, 0);
        grid.add(maxBidField, 1, 0);
        grid.add(new Label("Bước giá tự động:"), 0, 1);
        grid.add(incrementField, 1, 1);

        getDialogPane().setContent(grid);

        // Tự động focus con trỏ chuột vào ô nhập Giá trần
        Platform.runLater(() -> maxBidField.requestFocus());

        // 3. Xử lý logic và Validation khi người dùng bấm nút "Bật Auto-Bid"
        setResultConverter(dialogButton -> {
            if (dialogButton == confirmButtonType) {
                try {
                    // Lọc bỏ ký tự chữ, chỉ giữ lại số
                    double maxBid = Double.parseDouble(maxBidField.getText().replaceAll("[^0-9]", ""));
                    double increment = Double.parseDouble(incrementField.getText().replaceAll("[^0-9]", ""));

                    // Validation bắt lỗi
                    if (maxBid > currentBalance) {
                        showAlert("Giá trần không được vượt quá số dư trong ví!");
                        return null; // Trả về null để giữ hộp thoại không bị đóng
                    }
                    if (increment < minIncrement) {
                        showAlert("Bước giá tự động phải >= bước giá tối thiểu của phòng (" + cf.format(minIncrement) + ")!");
                        return null;
                    }
                    if (maxBid <= 0 || increment <= 0) {
                        showAlert("Các giá trị phải lớn hơn 0!");
                        return null;
                    }

                    // Nếu hợp lệ, đóng gói dữ liệu và gửi về Controller
                    return new AutoBidConfig(maxBid, increment);

                } catch (NumberFormatException e) {
                    showAlert("Vui lòng nhập đúng định dạng số hợp lệ!");
                    return null;
                }
            }
            return null; // Nếu bấm Hủy thì trả về null
        });
    }

    private void showAlert(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Lỗi cài đặt Auto-Bid");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
