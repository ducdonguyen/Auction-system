package com.auction.client.controller;
import com.auction.client.model.AuctionRoomViewModel;
import com.auction.client.service.AuctionRoomService;
import com.auction.client.util.SceneNavigator;
import com.auction.shared.network.ServiceResult;
import com.auction.client.network.SocketClient;
import com.auction.shared.models.*;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import java.io.IOException;
public class AuctionRoomController {
    private final AuctionRoomService service = new AuctionRoomService();
    private String aid;
    @FXML private Label auctionIdLabel, itemNameLabel, sellerLabel, statusLabel, currentPriceLabel, stepPriceLabel, minimumBidLabel, highestBidderLabel, scheduleLabel, messageLabel, descriptionLabel;
    @FXML private TextField bidAmountField;
    @FXML private ListView<String> bidHistoryList;
    public void setAuctionId(String auctionId) { this.aid = auctionId; render(); }
    @FXML
    public void initialize() {
        SocketClient.getInstance().setRealtimeListener(new SocketClient.RealtimeListener() {
            @Override
            public void onNewBid(BidTransaction bidTransaction) {
                currentPriceLabel.setText(String.format("%,.0f VNĐ", bidTransaction.bidAmount()));
                highestBidderLabel.setText(bidTransaction.bidder().getUsername());
                bidHistoryList.getItems().add(0, bidTransaction.bidder().getUsername() + " vừa đặt: " + bidTransaction.bidAmount());
            }
            @Override
            public void onStatusUpdate(AuctionStatus status) {
                statusLabel.setText(status.name());
                if (status == AuctionStatus.FINISHED || status == AuctionStatus.CANCELED) {
                    bidAmountField.setDisable(true); messageLabel.setText("Phiên đã kết thúc!");
                }
            }
        });
    }
    @FXML
    private void handlePlaceBidAction() {
        ServiceResult<AuctionRoomViewModel> result = service.placeBid(aid, bidAmountField.getText());
        messageLabel.setText(result.message());
        if (result.data() != null) bind(result.data());
    }
    @FXML
    private void handleBackAction() throws IOException {
        SceneNavigator.switchScene(bidAmountField, "/views/AuctionList.fxml", "Danh sách đấu giá", 1200, 760);
    }
    private void render() { service.getAuctionRoom(aid).ifPresent(result -> bind(result.data())); }
    private void bind(AuctionRoomViewModel viewModel) {
        auctionIdLabel.setText(viewModel.auctionId()); itemNameLabel.setText(viewModel.itemName()); sellerLabel.setText(viewModel.sellerName());
        statusLabel.setText(viewModel.status()); currentPriceLabel.setText(viewModel.currentPrice()); stepPriceLabel.setText(viewModel.stepPrice());
        minimumBidLabel.setText(viewModel.minimumBid()); highestBidderLabel.setText(viewModel.highestBidder()); scheduleLabel.setText(viewModel.schedule());
        descriptionLabel.setText(viewModel.description()); bidHistoryList.setItems(FXCollections.observableArrayList(viewModel.bidHistory()));
    }
}
