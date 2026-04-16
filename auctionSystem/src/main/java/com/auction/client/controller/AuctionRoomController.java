package com.auction.client.controller;
import com.auction.client.model.AuctionRoomViewModel;
import com.auction.client.model.ServiceResult;
import com.auction.client.service.AuctionRoomService;
import com.auction.client.util.SceneNavigator;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import java.io.IOException;
public class AuctionRoomController {
    private final AuctionRoomService service = new AuctionRoomService();
    private String auctionId;
    @FXML private Label auctionIdLabel, itemNameLabel, sellerLabel, statusLabel, currentPriceLabel, stepPriceLabel, minimumBidLabel, highestBidderLabel, scheduleLabel, messageLabel, descriptionLabel;
    @FXML private TextField bidAmountField; @FXML private ListView<String> bidHistoryList;
    public void setAuctionId(String id) { this.auctionId = id; render(); }
    @FXML private void handlePlaceBidAction() {
        ServiceResult<AuctionRoomViewModel> res = service.placeBid(auctionId, bidAmountField.getText());
        messageLabel.setText(res.message());
        if (res.data() != null) bind(res.data());
    }
    @FXML private void handleBackAction() throws IOException { SceneNavigator.switchScene(bidAmountField, "/views/AuctionList.fxml", "Danh sách đấu giá", 1200, 760); }
    private void render() { service.getAuctionRoom(auctionId).ifPresent(res -> bind(res.data())); }
    private void bind(AuctionRoomViewModel m) {
        auctionIdLabel.setText(m.auctionId()); itemNameLabel.setText(m.itemName());
        sellerLabel.setText(m.sellerName()); statusLabel.setText(m.status());
        currentPriceLabel.setText(m.currentPrice()); stepPriceLabel.setText(m.stepPrice());
        minimumBidLabel.setText(m.minimumBid()); highestBidderLabel.setText(m.highestBidder());
        scheduleLabel.setText(m.schedule()); descriptionLabel.setText(m.description());
        bidHistoryList.setItems(FXCollections.observableArrayList(m.bidHistory()));
    }
}
