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


    /**
     * Kỷ lục (Record) lưu trữ tham chiếu đến các trường nhập liệu của form.
     */


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
        // Clear session user on logout
        com.auction.client.service.SessionContext.setCurrentUser(null);
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

     */
    @FXML
    private void handleCreateAuctionAction(ActionEvent event) throws IOException {
        // Chuyển toàn bộ màn hình hiện tại sang màn hình nhập liệu tạo phiên
        SceneNavigator.switchScene(createAuctionButton, Scene.CREATE_AUCTION, null);
    }


    /**
     * Hàm tiện ích hiển thị nhanh một hộp thoại Alert thông báo.
     */
    public void showAlertDialog(Alert.AlertType type, String title, String content) {
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