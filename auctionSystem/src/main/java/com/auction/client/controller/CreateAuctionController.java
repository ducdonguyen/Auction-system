package com.auction.client.controller;

import com.auction.client.network.SocketClient;
import com.auction.client.service.SessionContext;
import com.auction.client.util.Scene;
import com.auction.client.util.SceneNavigator;
import com.auction.shared.network.requests.CreateAuctionRequest;
import com.auction.shared.network.responses.ServiceResult;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import java.io.IOException;
import java.time.LocalDateTime;

public class CreateAuctionController {

    @FXML private TextField nameField;
    @FXML private TextArea descriptionField;
    @FXML private TextField startingPriceField;
    @FXML private TextField priceStepField;
    @FXML private ComboBox<String> productTypeCombo;
    @FXML private TextField extraInfoField;
    @FXML private Button submitButton;

    // MỚI: controls cho thời lượng phiên
    @FXML private TextField durationField;           // số nguyên, ví dụ "3"
    @FXML private ComboBox<String> durationUnitCombo; // "Ngày", "Giờ", "Phút"

    @FXML
    public void initialize() {
        productTypeCombo.getItems().addAll("Điện tử", "Tác phẩm nghệ thuật", "Xe cộ", "Thời trang", "Khác");
        productTypeCombo.setValue("Điện tử");

        // Khởi tạo combobox đơn vị thời lượng (nếu được injection từ FXML)
        if (durationUnitCombo != null) {
            durationUnitCombo.getItems().addAll("Ngày", "Giờ", "Phút");
            durationUnitCombo.setValue("Ngày");
        }

        // Optionally set prompt text for durationField (if injected)
        if (durationField != null) {
            durationField.setPromptText("3");
        }
    }

    /**
     * Xử lý khi người dùng bấm nút "Tạo phiên" trên màn hình
     */
    @FXML
    private void handleCreateAction(ActionEvent event) {
        submitButton.setDisable(true);

        // Validate và parse các trường số một cách an toàn
        double startingPrice;
        double priceStep;
        try {
            startingPrice = Double.parseDouble(startingPriceField.getText().trim());
            priceStep = Double.parseDouble(priceStepField.getText().trim());
            if (startingPrice < 0 || priceStep <= 0) {
                submitButton.setDisable(false);
                showAlert(Alert.AlertType.ERROR, "Dữ liệu không hợp lệ", "Giá khởi điểm >= 0 và bước giá phải > 0.");
                return;
            }
        } catch (NumberFormatException ex) {
            submitButton.setDisable(false);
            showAlert(Alert.AlertType.ERROR, "Dữ liệu không hợp lệ", "Giá khởi điểm và bước giá phải là số.");
            return;
        }

        // Lấy và kiểm tra thời lượng
        long durationValue = 3;
        String durationUnit = "Ngày";
        try {
            String durText = (durationField != null) ? durationField.getText().trim() : "";
            if (durText == null || durText.isEmpty()) {
                // Mặc định 3 ngày nếu rỗng
                durationValue = 3L;
            } else {
                durationValue = Long.parseLong(durText);
                if (durationValue <= 0) {
                    submitButton.setDisable(false);
                    showAlert(Alert.AlertType.ERROR, "Dữ liệu không hợp lệ", "Thời lượng phải là số nguyên dương.");
                    return;
                }
            }
        } catch (NumberFormatException ex) {
            submitButton.setDisable(false);
            showAlert(Alert.AlertType.ERROR, "Dữ liệu không hợp lệ", "Thời lượng phải là một số nguyên.");
            return;
        }

        durationUnit = (durationUnitCombo != null && durationUnitCombo.getValue() != null)
                ? durationUnitCombo.getValue()
                : "Ngày";

        // Chuyển loại sản phẩm sang serverType hiện tại
        String serverType = switch (productTypeCombo.getValue()) {
            case "Điện tử" -> "ELECTRONICS";
            case "Xe cộ" -> "VEHICLE";
            case "Tác phẩm nghệ thuật", "Thời trang" -> "ART";
            default -> "OTHER";
        };

        LocalDateTime startTime = LocalDateTime.now();

        // Tính endTime theo đơn vị
        LocalDateTime endTime;
        switch (durationUnit) {
            case "Giờ" -> endTime = startTime.plusHours(durationValue);
            case "Phút" -> endTime = startTime.plusMinutes(durationValue);
            case "Ngày" -> endTime = startTime.plusDays(durationValue);
            default -> endTime = startTime.plusDays(3);
        }

        CreateAuctionRequest request = new CreateAuctionRequest(
                nameField.getText().trim(),
                descriptionField.getText().trim(),
                startingPrice,
                priceStep,
                serverType,
                extraInfoField.getText().trim(),
                SessionContext.getCurrentUser() != null ? SessionContext.getCurrentUser().getUsername() : "Admin",
                startTime,
                endTime
        );

        Task<ServiceResult<?>> task = new Task<>() {
            @Override
            protected ServiceResult<?> call() throws Exception {
                SocketClient.getInstance().sendRequest(request);
                return (ServiceResult<?>) SocketClient.getInstance().receiveResponse();
            }
        };

        task.setOnSucceeded(e -> {
            Platform.runLater(() -> {
                ServiceResult<?> result = task.getValue();
                if (result.success()) {
                    // Hiện thông báo thành công
                    showAlert(Alert.AlertType.INFORMATION, "Thành công", result.message());
                    // Quay trở lại màn hình danh sách sau khi tạo xong
                    goToCatalog();
                } else {
                    submitButton.setDisable(false);
                    showAlert(Alert.AlertType.ERROR, "Thất bại", result.message());
                }
            });
        });

        task.setOnFailed(e -> Platform.runLater(() -> {
            submitButton.setDisable(false);
            showAlert(Alert.AlertType.ERROR, "Lỗi kết nối", "Không thể gửi yêu cầu lên Server!");
        }));

        new Thread(task).start();
    }

    /**
     * Bấm nút "Hủy" hoặc "Quay lại"
     */
    @FXML
    private void handleCancelAction(ActionEvent event) {
        goToCatalog();
    }

    private void goToCatalog() {
        try {
            // Sử dụng bộ Navigator của bạn để quay về màn hình danh sách chính
            SceneNavigator.switchScene(submitButton, Scene.AUCTION_LIST);
        } catch (IOException e) {
            System.err.println("Không thể chuyển màn hình: " + e.getMessage());
        }
    }

    private void showAlert(Alert.AlertType type, String title, String content) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }
}