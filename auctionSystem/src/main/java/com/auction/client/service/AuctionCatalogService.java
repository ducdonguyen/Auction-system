package com.auction.client.service;

import com.auction.shared.models.Auction;
import com.auction.shared.models.AuctionRow;
import java.text.NumberFormat;
import java.util.List;
import java.util.Locale;

/**
 * Service xử lý danh mục đấu giá, hỗ trợ lọc và định dạng dữ liệu cho TableView.
 */
public class AuctionCatalogService {
    private final NumberFormat currencyFormat = NumberFormat.getCurrencyInstance(new Locale("vi", "VN"));

    public List<String> getAvailableStatuses() {
        return List.of("Tất cả", "OPEN", "RUNNING", "FINISHED", "PAID", "CANCELED");
    }

    public List<AuctionRow> filterAuctions(String keyword, String status) {
        String kw = (keyword == null) ? "" : keyword.trim().toLowerCase(Locale.ROOT);
        return AuctionDataStore.getAuctions().stream()
                .filter(a -> kw.isEmpty() || a.getAuctionId().toLowerCase().contains(kw) || a.getItem().getName().toLowerCase().contains(kw))
                .filter(a -> status == null || "Tất cả".equals(status) || a.getStatus().name().equalsIgnoreCase(status))
                .map(this::toRow)
                .toList();
    }

    private AuctionRow toRow(Auction a) {
        String bidder = (a.getHighestBidder() == null) ? "Chưa có" : a.getHighestBidder().getUsername();
        String summary = a.getItem().getDescription() + " | Người dẫn đầu: " + bidder;
        return new AuctionRow(
                a.getAuctionId(),
                a.getItem().getName(),
                a.getSeller().getUsername(),
                currencyFormat.format(a.getCurrentPrice()),
                currencyFormat.format(a.getStepPrice()),
                a.getStatus().name(),
                summary,
                bidder
        );
    }
}
