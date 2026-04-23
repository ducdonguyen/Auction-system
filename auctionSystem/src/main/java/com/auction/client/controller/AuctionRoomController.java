package com.auction.client.controller;

import com.auction.client.model.AuctionRoomViewModel;
import com.auction.client.service.AuctionRoomService;
import com.auction.client.util.SceneNavigator;
import com.auction.shared.network.ServiceResult;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;

import java.io.IOException;

public class AuctionRoomController {
    private final AuctionRoomService service = new AuctionRoomService();
    private String auctionId;
    @FXML
    private Label auctionIdLabel;
    @FXML
    private Label itemNameLabel;
    @FXML
    private Label sellerLabel;
    @FXML
    private Label statusLabel;
    @FXML
    private Label currentPriceLabel;
    @FXML
    private Label stepPriceLabel;
    @FXML
    private Label minimumBidLabel;
    @FXML
    private Label highestBidderLabel;
    @FXML
    private Label scheduleLabel;
    @FXML
    private Label messageLabel;
    @FXML
    private Label descriptionLabel;
    @FXML
    private TextField bidAmountField;
    @FXML
    private ListView<String> bidHistoryList;

    public void setAuctionId(String id) {
        this.auctionId = id;
        render();
    }

    @FXML
    private void handlePlaceBidAction() {
        ServiceResult<AuctionRoomViewModel> res = service.placeBid(auctionId, bidAmountField.getText());
        messageLabel.setText(res.message());
        if (res.data() != null) {
            bind(res.data());
        }
    }

    @FXML
    private void handleBackAction() throws IOException {
        SceneNavigator.switchScene(bidAmountField, "/views/AuctionList.fxml", "Danh sách đấu giá", 1200, 760);
    }

    private void render() {
        service.getAuctionRoom(auctionId).ifPresent(res -> bind(res.data()));
    }

    private void bind(AuctionRoomViewModel viewModel) {
        auctionIdLabel.setText(viewModel.auctionId());
        itemNameLabel.setText(viewModel.itemName());
        sellerLabel.setText(viewModel.sellerName());
        statusLabel.setText(viewModel.status());
        currentPriceLabel.setText(viewModel.currentPrice());
        stepPriceLabel.setText(viewModel.stepPrice());
        minimumBidLabel.setText(viewModel.minimumBid());
        highestBidderLabel.setText(viewModel.highestBidder());
        scheduleLabel.setText(viewModel.schedule());
        descriptionLabel.setText(viewModel.description());
        bidHistoryList.setItems(FXCollections.observableArrayList(viewModel.bidHistory()));
    }
}
