package com.auction.client.controller;

import com.auction.client.service.SessionContext;
import com.auction.client.util.Scene;
import com.auction.client.util.SceneNavigator;
import com.auction.shared.network.requests.CreateAuctionRequest;
import com.auction.client.network.SocketClient;
import com.auction.client.service.AuctionCatalogService;
import com.auction.shared.models.auction.AuctionRow;
import com.auction.shared.network.responses.ServiceResult;
import com.auction.client.view.TopUpDialog;
import com.auction.shared.network.requests.TopUpRequest;
import com.auction.shared.network.responses.TopUpResponse;
import java.io.IOException;
import java.util.Optional;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;

/**
 * Controller cho màn hình danh sách các phiên đấu giá
 */
public class AuctionListController {
    private final AuctionCatalogService service = new AuctionCatalogService();
    private final ObservableList<AuctionRow> data = FXCollections.observableArrayList();

    // BIẾN LƯU TRỮ TRẠNG THÁI SỐ DƯ MỚI THÊM
    private double currentBalance = 0.0;

    @FXML
    private TextField searchField;
    @FXML
    private ComboBox<String> statusFilter;
    @FXML
    private TableView<AuctionRow> auctionTable;
    @FXML
    private Label resultLabel;
    @FXML
    private Button openAuctionButton;
    @FXML
    private Button createAuctionButton;
    @FXML
    private Button balanceButton;
    @FXML
    private TableColumn<AuctionRow, String> idColumn;
    @FXML
    private TableColumn<AuctionRow, String> itemColumn;
    @FXML
    private TableColumn<AuctionRow, String> sellerColumn;
    @FXML
    private TableColumn<AuctionRow, String> highestBidderColumn;
    @FXML
    private TableColumn<AuctionRow, String> priceColumn;
    @FXML
    private TableColumn<AuctionRow, String> stepColumn;
    @FXML
    private TableColumn<AuctionRow, String> summaryColumn;
    @FXML
    private TableColumn<AuctionRow, String> statusColumn;

    /**
     * Exception tùy chỉnh dùng để ném ra khi dữ liệu nhập vào form không hợp lệ.
     */
    private static class ValidationException extends Exception {
        public ValidationException(String message) {
            super(message);
        }
    }

    /**
     * Kỷ lục (Record) lưu trữ tham chiếu đến các trường nhập liệu của form.
     */
    private record AuctionFormInputs(
            TextField txtName, TextArea txtDescription, TextField txtStartingPrice,
            TextField txtPriceStep, ComboBox<String> cbProductType, TextField txtExtraInfo
    ) {}

    /**
     * Khởi tạo controller, thiết lập các cột cho bảng và tải dữ liệu.
     */
    @FXML
    public void initialize() {
        statusFilter.setItems(FXCollections.observableArrayList(service.getAvailableStatuses()));
        statusFilter.setValue("Tất cả");

        idColumn.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().auctionId()));
        itemColumn.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().itemName()));
        sellerColumn.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().sellerName()));
        highestBidderColumn.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().highestBidder()));
        priceColumn.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().currentPrice()));
        stepColumn.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().stepPrice()));
        statusColumn.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().status()));
        summaryColumn.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().summary()));

        auctionTable.setItems(data);

        // 1. Cập nhật số dư hiển thị ban đầu lên nút xanh
        if (com.auction.client.service.SessionContext.getCurrentUser() != null) {
            this.currentBalance = com.auction.client.service.SessionContext.getCurrentUser().getBalance();
        }
        updateBalanceUI(currentBalance);

        // 2. ĐĂNG KÝ BỘ LẮNG NGHE MẠNG REAL-TIME: Đón gói tin phản hồi nạp tiền từ Server
        SocketClient.getInstance().registerResponseHandler(TopUpResponse.class, (Object response) -> {
            // Ép kiểu an toàn sau khi đã chỉ định tham số là Object
            if (response instanceof TopUpResponse res) {

                // Đẩy xử lý về UI Thread của JavaFX để cập nhật giao diện an toàn
                Platform.runLater(() -> {
                    if (res.isSuccess()) {
                        this.currentBalance = res.getNewBalance();
                        updateBalanceUI(this.currentBalance); // Thay đổi con số trên màn hình ngay lập tức

                        if (com.auction.client.service.SessionContext.getCurrentUser() != null) {
                            com.auction.client.service.SessionContext.getCurrentUser().setBalance(res.getNewBalance());
                        }

                        showAlertDialog(Alert.AlertType.INFORMATION, "Thành công", "Nạp tiền thành công: " + res.getMessage());
                    } else {
                        showAlertDialog(Alert.AlertType.ERROR, "Thất bại", "Nạp tiền thất bại: " + res.getMessage());
                    }
                });
            }
        });

        reload();
    }

    @FXML
    private void handleSearchAction() {
        reload();
    }

    @FXML
    private void handleRefreshAction() {
        reload();
    }

    @FXML
    private void handleOpenAuctionAction() throws IOException {
        AuctionRow sel = auctionTable.getSelectionModel().getSelectedItem();
        if (sel != null) {
            SceneNavigator.switchScene(openAuctionButton, Scene.AUCTION_ROOM,
                    (AuctionRoomController c) -> c.setAuctionId(sel.auctionId()));
        }
    }

    @FXML
    private void handleLogoutAction() throws IOException {
        SceneNavigator.switchScene(openAuctionButton, Scene.LOGIN, null);
    }

    /**
     * Xử lý sự kiện khi bấm ô Số dư màu xanh biển.
     * Hiển thị popup mốc tiền nạp nhanh và gửi yêu cầu Socket.
     */
    @FXML
    private void handleOpenTopUpDialog() {
        TopUpDialog dialog = new TopUpDialog(amount -> {});
        Optional<Double> result = dialog.showAndWait();

        result.ifPresent(amount -> {
            if (amount <= 0) {
                showAlertDialog(Alert.AlertType.WARNING, "Cảnh báo", "Số tiền nạp không hợp lệ");
                return;
            }

            try {
                // Lấy thông tin định danh người dùng hiện tại từ Session hệ thống
                String userName = "Chưa xác định";
                if (com.auction.client.service.SessionContext.getCurrentUser() != null) {
                    userName = SessionContext.getCurrentUser().getUsername();
                }

                // Đóng gói request gửi bất đồng bộ lên Server qua đường truyền mạng Socket
                TopUpRequest request = new TopUpRequest(userName, amount);
                SocketClient.getInstance().sendRequest(request);

            } catch (Exception e) {
                showAlertDialog(Alert.AlertType.ERROR, "Lỗi kết nối", "Không thể gửi yêu cầu nạp tiền: " + e.getMessage());
            }
        });
    }

    /**
     * Định dạng tiền tệ hiển thị sắc nét trên ô màu xanh (Ví dụ: Số dư: 500,000 đ)
     */
    private void updateBalanceUI(double balance) {
        if (balanceButton != null) {
            balanceButton.setText(String.format("Số dư: %,.0f đ", balance));
        }
    }

    /**
     * Bắt sự kiện khi click vào nút "Tạo phiên đấu giá".
     * Đã được refactor tuân thủ nguyên tắc SRP (Single Responsibility Principle).
     */
    @FXML
    private void handleCreateAuctionAction(ActionEvent event) {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Tạo phiên đấu giá mới");
        dialog.setHeaderText("Nhập thông tin sản phẩm đấu giá");

        ButtonType confirmButtonType = new ButtonType("Xác nhận", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(confirmButtonType, ButtonType.CANCEL);

        // 1. Khởi tạo giao diện và lấy về các tham chiếu trường nhập liệu
        AuctionFormInputs formInputs = setupAuctionFormLayout(dialog);

        final Button btnConfirm = (Button) dialog.getDialogPane().lookupButton(confirmButtonType);

        // 2. Can thiệp sự kiện nút "Xác nhận"
        btnConfirm.addEventFilter(ActionEvent.ACTION, ae -> {
            try {
                // Kiểm tra và đóng gói dữ liệu
                CreateAuctionRequest request = validateAndBuildRequest(formInputs);

                // Giữ Dialog mở chờ Server và khóa nút Xác nhận
                ae.consume();
                btnConfirm.setDisable(true);

                // Ném Request vào luồng Background xử lý bất đồng bộ
                submitCreateAuctionAsync(request, dialog, confirmButtonType, btnConfirm);

            } catch (ValidationException e) {
                showAlertDialog(Alert.AlertType.ERROR, "Lỗi nhập liệu", e.getMessage());
                ae.consume(); // Giữ nguyên Dialog nếu có lỗi nhập liệu
            } catch (Exception e) {
                showAlertDialog(Alert.AlertType.ERROR, "Lỗi hệ thống", "Lỗi không xác định: " + e.getMessage());
                ae.consume();
            }
        });

        dialog.showAndWait();
    }

    /**
     * Helper 1: Xây dựng Layout bằng GridPane và khởi tạo các UI Controls.
     */
    private AuctionFormInputs setupAuctionFormLayout(Dialog<ButtonType> dialog) {
        GridPane grid = new GridPane();
        grid.setHgap(12); grid.setVgap(12);
        grid.setPadding(new Insets(20, 30, 10, 30));

        TextField txtName = new TextField(); txtName.setPromptText("Tên sản phẩm"); txtName.setPrefWidth(260);
        TextArea txtDescription = new TextArea(); txtDescription.setPromptText("Mô tả chi tiết..."); txtDescription.setPrefRowCount(3); txtDescription.setWrapText(true);
        TextField txtStartingPrice = new TextField(); txtStartingPrice.setPromptText("Ví dụ: 200000");
        TextField txtPriceStep = new TextField(); txtPriceStep.setPromptText("Ví dụ: 20000");
        TextField txtExtraInfo = new TextField(); txtExtraInfo.setPromptText("Ví dụ: 12, 24");

        Label lblExtraInfo = new Label("Bảo hành (tháng):");
        lblExtraInfo.setWrapText(true); lblExtraInfo.setPrefWidth(120);
        lblExtraInfo.setMinHeight(javafx.scene.layout.Region.USE_PREF_SIZE);

        ComboBox<String> cbProductType = new ComboBox<>();
        cbProductType.getItems().addAll("Điện tử", "Tác phẩm nghệ thuật", "Xe cộ", "Thời trang", "Khác");
        cbProductType.setValue("Điện tử");
        cbProductType.setMaxWidth(Double.MAX_VALUE);

        cbProductType.valueProperty().addListener((observable, oldValue, newValue) -> {
            switch (newValue) {
                case "Tác phẩm nghệ thuật", "Thời trang" -> { lblExtraInfo.setText("Tên tác giả:"); txtExtraInfo.setPromptText("Ví dụ: Picasso..."); }
                case "Xe cộ" -> { lblExtraInfo.setText("Hãng xe:"); txtExtraInfo.setPromptText("Ví dụ: Toyota..."); }
                case "Khác" -> { lblExtraInfo.setText("Chi tiết loại:"); txtExtraInfo.setPromptText("Ví dụ: Thú cưng..."); }
                default -> { lblExtraInfo.setText("Bảo hành (tháng):"); txtExtraInfo.setPromptText("Ví dụ: 12, 24"); }
            }
        });

        grid.add(new Label("Tên sản phẩm:"), 0, 0); grid.add(txtName, 1, 0);
        grid.add(new Label("Mô tả:"), 0, 1);        grid.add(txtDescription, 1, 1);
        grid.add(new Label("Giá khởi điểm:"), 0, 2); grid.add(txtStartingPrice, 1, 2);
        grid.add(new Label("Bước giá:"), 0, 3);      grid.add(txtPriceStep, 1, 3);
        grid.add(new Label("Loại sản phẩm:"), 0, 4); grid.add(cbProductType, 1, 4);
        grid.add(lblExtraInfo, 0, 5);                grid.add(txtExtraInfo, 1, 5);

        dialog.getDialogPane().setContent(grid);

        return new AuctionFormInputs(txtName, txtDescription, txtStartingPrice, txtPriceStep, cbProductType, txtExtraInfo);
    }

    /**
     * Helper 2: Xác thực dữ liệu và chuyển đổi thành đối tượng CreateAuctionRequest.
     */
    private CreateAuctionRequest validateAndBuildRequest(AuctionFormInputs inputs) throws ValidationException {
        String name = inputs.txtName().getText().trim();
        String desc = inputs.txtDescription().getText().trim();
        String startPriceStr = inputs.txtStartingPrice().getText().trim();
        String priceStepStr = inputs.txtPriceStep().getText().trim();
        String type = inputs.cbProductType().getValue();
        String extraInfo = inputs.txtExtraInfo().getText().trim();

        if (name.isEmpty() || desc.isEmpty() || startPriceStr.isEmpty() || priceStepStr.isEmpty() || type == null) {
            throw new ValidationException("Vui lòng nhập đầy đủ tất cả các trường dữ liệu bắt buộc!");
        }
        if (extraInfo.isEmpty()) {
            throw new ValidationException("Vui lòng nhập thông tin Phụ (Tác giả / Hãng xe / Bảo hành)!");
        }

        double startingPrice;
        double priceStep;
        try {
            startingPrice = Double.parseDouble(startPriceStr);
            priceStep = Double.parseDouble(priceStepStr);
            if (startingPrice <= 0 || priceStep <= 0) {
                throw new ValidationException("Giá khởi điểm và Bước giá phải là số lớn hơn 0!");
            }
        } catch (NumberFormatException e) {
            throw new ValidationException("Giá khởi điểm và Bước giá bắt buộc phải nhập ký tự số!");
        }

        String serverType = switch (type) {
            case "Điện tử" -> "ELECTRONICS";
            case "Xe cộ" -> "VEHICLE";
            case "Tác phẩm nghệ thuật", "Thời trang" -> "ART";
            default -> "OTHER";
        };

        if (serverType.equals("ELECTRONICS")) {
            try {
                Integer.parseInt(extraInfo);
            } catch (NumberFormatException e) {
                throw new ValidationException("Số tháng bảo hành của đồ Điện tử bắt buộc phải nhập con số!");
            }
        }

        String sellerUsername = com.auction.client.service.SessionContext.getCurrentUser().getFullName();
        java.time.LocalDateTime startTime = java.time.LocalDateTime.now();
        java.time.LocalDateTime endTime = startTime.plusDays(3);

        return new CreateAuctionRequest(name, desc, startingPrice, priceStep, serverType, extraInfo, sellerUsername, startTime, endTime);
    }

    /**
     * Helper 3: Gửi yêu cầu qua Socket bất đồng bộ và xử lý phản hồi.
     */
    private void submitCreateAuctionAsync(CreateAuctionRequest request, Dialog<ButtonType> dialog,
                                          ButtonType confirmButtonType, Button btnConfirm) {
        javafx.concurrent.Task<ServiceResult<?>> task = new javafx.concurrent.Task<>() {
            @Override
            protected ServiceResult<?> call() throws Exception {
                SocketClient.getInstance().sendRequest(request);
                return (ServiceResult<?>) SocketClient.getInstance().receiveResponse();
            }
        };

        task.setOnSucceeded(eventTask -> {
            btnConfirm.setDisable(false);
            ServiceResult<?> result = task.getValue();
            if (result.success()) {
                showAlertDialog(Alert.AlertType.INFORMATION, "Thành công", result.message());
                dialog.setResult(confirmButtonType);
                dialog.close();
            } else {
                showAlertDialog(Alert.AlertType.ERROR, "Lỗi từ Server", result.message());
            }
        });

        task.setOnFailed(eventTask -> {
            btnConfirm.setDisable(false);
            Throwable ex = task.getException();
            showAlertDialog(Alert.AlertType.ERROR, "Lỗi kết nối", "Không thể giao tiếp với Server: " + ex.getMessage());
        });

        new Thread(task).start();
    }

    /**
     * Hàm tiện ích hiển thị nhanh một hộp thoại Alert thông báo.
     */
    private void showAlertDialog(Alert.AlertType type, String title, String content) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }

    private void reload() {
        data.setAll(service.filterAuctions(searchField.getText(), statusFilter.getValue()));
        resultLabel.setText("Hiển thị " + data.size() + " phiên.");
    }
}