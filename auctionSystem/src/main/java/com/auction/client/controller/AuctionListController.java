package com.auction.client.controller;

import com.auction.shared.network.CreateAuctionRequest;
import com.auction.client.network.SocketClient;
import com.auction.client.service.AuctionCatalogService;
import com.auction.client.util.SceneNavigator;
import com.auction.shared.models.AuctionRow;
import java.io.IOException;
import java.time.LocalDateTime;

import com.auction.shared.network.ServiceResult;
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

    // Ánh xạ nút "Tạo phiên đấu giá" từ file FXML
    @FXML
    private Button createAuctionButton;

    @FXML
    private TableColumn<AuctionRow, String> idColumn;
    @FXML
    private TableColumn<AuctionRow, String> itemColumn;
    @FXML
    private TableColumn<AuctionRow, String> sellerColumn;
    @FXML
    private TableColumn<AuctionRow, String> priceColumn;
    @FXML
    private TableColumn<AuctionRow, String> stepColumn;
    @FXML
    private TableColumn<AuctionRow, String> summaryColumn;
    @FXML
    private TableColumn<AuctionRow, String> statusColumn;

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
        priceColumn.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().currentPrice()));
        stepColumn.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().stepPrice()));
        statusColumn.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().status()));
        summaryColumn.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().summary()));
        auctionTable.setItems(data);
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
            SceneNavigator.<AuctionRoomController>switchScene(openAuctionButton,
                    "/views/AuctionRoom.fxml", "Phòng đấu giá", 1180, 780,
                    c -> c.setAuctionId(sel.auctionId()));
        }
    }

    @FXML
    private void handleLogoutAction() throws IOException {
        SceneNavigator.switchScene(openAuctionButton, "/views/Login.fxml", "Đăng nhập", 980, 640);
    }

    /**
     * Bắt sự kiện khi click vào nút "Tạo phiên đấu giá".
     * Bung Dialog form nhập liệu, validate dữ liệu và ném request qua SocketClient.
     */
    @FXML
    private void handleCreateAuctionAction(ActionEvent event) {
        // 1. Tạo Khung Dialog
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Tạo phiên đấu giá mới");
        dialog.setHeaderText("Nhập thông tin sản phẩm đấu giá");

        // Tạo tổ hợp nút bấm Xác nhận và Hủy
        ButtonType confirmButtonType = new ButtonType("Xác nhận", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(confirmButtonType, ButtonType.CANCEL);

        // 2. Dựng Layout Form nhập liệu bằng GridPane
        GridPane grid = new GridPane();
        grid.setHgap(12);
        grid.setVgap(12);
        grid.setPadding(new Insets(20, 30, 10, 30));

        TextField txtName = new TextField();
        txtName.setPromptText("Tên sản phẩm");
        txtName.setPrefWidth(260);

        TextArea txtDescription = new TextArea();
        txtDescription.setPromptText("Mô tả chi tiết về sản phẩm...");
        txtDescription.setPrefRowCount(3);
        txtDescription.setWrapText(true);

        TextField txtStartingPrice = new TextField();
        txtStartingPrice.setPromptText("Ví dụ: 200000");

        TextField txtPriceStep = new TextField();
        txtPriceStep.setPromptText("Ví dụ: 20000");

        Label lblExtraInfo = new Label("Bảo hành (tháng):");
        lblExtraInfo.setWrapText(true);
        lblExtraInfo.setPrefWidth(120);
        lblExtraInfo.setMinHeight(javafx.scene.layout.Region.USE_PREF_SIZE);

        TextField txtExtraInfo = new TextField();
        txtExtraInfo.setPromptText("Ví dụ: 12, 24");

        ComboBox<String> cbProductType = new ComboBox<>();
        cbProductType.getItems().addAll("Điện tử", "Tác phẩm nghệ thuật", "Xe cộ", "Thời trang", "Khác");
        cbProductType.setValue("Điện tử"); // Giá trị mặc định
        cbProductType.setMaxWidth(Double.MAX_VALUE);

        cbProductType.valueProperty().addListener((observable, oldValue, newValue) -> {
            switch (newValue) {
                case "Tác phẩm nghệ thuật", "Thời trang" -> {
                    lblExtraInfo.setText("Tên tác giả (hoặc nhà sản xuất):");
                    txtExtraInfo.setPromptText("Ví dụ: Picasso, Chanel...");
                }
                case "Xe cộ" -> {
                    lblExtraInfo.setText("Hãng xe:");
                    txtExtraInfo.setPromptText("Ví dụ: Toyota, Honda...");
                }
                case "Khác" -> {
                    lblExtraInfo.setText("Chi tiết loại sản phẩm:");
                    txtExtraInfo.setPromptText("Ví dụ: Bất động sản, Thú cưng...");
                }
                default -> { // Điện tử
                    lblExtraInfo.setText("Bảo hành (tháng):");
                    txtExtraInfo.setPromptText("Ví dụ: 12, 24");
                }
            }
        });

        // Sắp xếp các thành phần vào lưới tọa độ (Cột, Hàng)
        grid.add(new Label("Tên sản phẩm:"), 0, 0);
        grid.add(txtName, 1, 0);
        grid.add(new Label("Mô tả:"), 0, 1);
        grid.add(txtDescription, 1, 1);
        grid.add(new Label("Giá khởi điểm:"), 0, 2);
        grid.add(txtStartingPrice, 1, 2);
        grid.add(new Label("Bước giá:"), 0, 3);
        grid.add(txtPriceStep, 1, 3);
        grid.add(new Label("Loại sản phẩm:"), 0, 4);
        grid.add(cbProductType, 1, 4);
        grid.add(lblExtraInfo, 0, 5);
        grid.add(txtExtraInfo, 1, 5);

        dialog.getDialogPane().setContent(grid);

        // 3. Can thiệp sự kiện nút "Xác nhận" để kiểm tra tính hợp lệ dữ liệu (Validation)
        final Button btnConfirm = (Button) dialog.getDialogPane().lookupButton(confirmButtonType);
        btnConfirm.addEventFilter(ActionEvent.ACTION, ae -> {
            String name = txtName.getText().trim();
            String desc = txtDescription.getText().trim();
            String startPriceStr = txtStartingPrice.getText().trim();
            String priceStepStr = txtPriceStep.getText().trim();
            String type = cbProductType.getValue();

            // KIỂM TRA RỖNG
            if (name.isEmpty() || desc.isEmpty() || startPriceStr.isEmpty() || priceStepStr.isEmpty() || type == null) {
                showAlertDialog(Alert.AlertType.ERROR, "Lỗi nhập liệu", "Vui lòng nhập đầy đủ tất cả các trường dữ liệu!");
                ae.consume(); // Ngăn chặn không cho đóng Dialog
                return;
            }

            // KIỂM TRA ĐỊNH DẠNG SỐ VÀ GIÁ TRỊ HỢP LỆ
            try {
                double startingPrice = Double.parseDouble(startPriceStr);
                double priceStep = Double.parseDouble(priceStepStr);

                if (startingPrice <= 0 || priceStep <= 0) {
                    showAlertDialog(Alert.AlertType.ERROR, "Lỗi giá trị", "Giá khởi điểm và Bước giá phải là số lớn hơn 0!");
                    ae.consume(); // Giữ nguyên Dialog
                    return;
                }

                // 4. ĐÓNG GÓI THÀNH REQUEST DTO
                // 4.1. Lấy tên người dùng đang đăng nhập
                String sellerUsername = com.auction.client.service.SessionContext.getCurrentUser().getFullName();

                // 4.2. Thời gian mặc định (Ví dụ: Bắt đầu ngay lúc này, kết thúc sau 3 ngày)
                java.time.LocalDateTime startTime = java.time.LocalDateTime.now();
                java.time.LocalDateTime endTime = startTime.plusDays(3);

                // 4.3. Chuyển đổi Loại sản phẩm (Tiếng Việt) sang Mã Server (Tiếng Anh)
                String serverType = switch (type) {
                    case "Điện tử" -> "ELECTRONICS";
                    case "Xe cộ" -> "VEHICLE";
                    case "Tác phẩm nghệ thuật", "Thời trang" -> "ART";
                    case "Khác" -> "OTHER";
                    default -> "OTHER";
                };

                // 4.4. Lấy giá trị extraInfo do người dùng tự nhập
                String extraInfo = txtExtraInfo.getText().trim();

                // KIỂM TRA RỖNG CHO THÔNG TIN PHỤ
                if (extraInfo.isEmpty()) {
                    showAlertDialog(Alert.AlertType.ERROR, "Thiếu thông tin", "Vui lòng nhập thông tin Tác giả / Hãng xe / Số tháng bảo hành!");
                    ae.consume();
                    return;
                }

                // KIỂM TRA LOGIC ĐIỆN TỬ (Phải là số nguyên)
                if (serverType.equals("ELECTRONICS")) {
                    try {
                        Integer.parseInt(extraInfo); // Thử ép kiểu xem có phải là số không
                    } catch (NumberFormatException e) {
                        showAlertDialog(Alert.AlertType.ERROR, "Lỗi định dạng", "Số tháng bảo hành bắt buộc phải nhập con số!");
                        ae.consume();
                        return;
                    }
                }

                // 5. ĐÓNG GÓI THÀNH REQUEST DTO (Bản đầy đủ 9 tham số)
                CreateAuctionRequest request = new CreateAuctionRequest(
                        name, desc, startingPrice, priceStep, serverType, extraInfo, sellerUsername, startTime, endTime
                );

                // 6. GỬI LÊN SERVER VÀ ĐỢI KẾT QUẢ
                com.auction.client.network.SocketClient.getInstance().sendRequest(request);

                // Bắt buộc phải đọc gói tin Server trả về để thông luồng mạng
                ServiceResult<?> result = (ServiceResult<?>) com.auction.client.network.SocketClient.getInstance().receiveResponse();

                if (result.success()) {
                    // Hiện thông báo thành công lấy từ Server ("Đã gửi yêu cầu tạo phòng...")
                    showAlertDialog(Alert.AlertType.INFORMATION, "Thành công", result.message());
                } else {
                    showAlertDialog(Alert.AlertType.ERROR, "Lỗi từ Server", result.message());
                    ae.consume(); // Chặn đóng Dialog để người dùng sửa lại
                }

            } catch (NumberFormatException e) {
                showAlertDialog(Alert.AlertType.ERROR, "Lỗi định dạng", "Giá khởi điểm và Bước giá bắt buộc phải nhập ký tự số!");
                ae.consume();
            } catch (Exception e) {
                // Đã đổi thành Exception chung để bắt cả lỗi ép kiểu Socket
                showAlertDialog(Alert.AlertType.ERROR, "Lỗi kết nối mạng", "Không thể giao tiếp với Server: " + e.getMessage());
                ae.consume();
            }
        });

        // Hiển thị Dialog lên màn hình
        dialog.showAndWait();
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
