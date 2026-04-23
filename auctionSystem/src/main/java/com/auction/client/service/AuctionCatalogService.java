package com.auction.client.service;

import com.auction.shared.models.Auction;
import com.auction.shared.models.AuctionRow;

import java.text.NumberFormat;
import java.util.ArrayList;
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
        String kw = (keyword == null) ? "" : keyword.toLowerCase().trim();
        boolean isFilterAll = (status == null || "Tất cả".equals(status));

        List<AuctionRow> result = new ArrayList<>();
        for (Auction a : AuctionDataStore.getAuctions()) {
            if (matchesKeyword(a, kw)) {
                if (isFilterAll || a.getStatus().name().equalsIgnoreCase(status)) {
                    result.add(toRow(a)); // map + collect
                }
            }
        }
        return result;
    }

    private boolean matchesKeyword(Auction a, String kw) {
        if (kw.isEmpty()) {
            return true;
        }
        return a.getAuctionId().toLowerCase().contains(kw)
                || a.getItem().getName().toLowerCase().contains(kw);
    }

    private AuctionRow toRow(Auction a) {
        String bidder = (a.getHighestBidder() == null) ? "Chưa có" : a.getHighestBidder().getUsername();

        return new AuctionRow(
                a.getAuctionId(),
                a.getItem().getName(),
                a.getSeller().getUsername(),
                currencyFormat.format(a.getCurrentPrice()),
                currencyFormat.format(a.getStepPrice()),
                a.getStatus().name(),
                String.format("%s | Người dẫn đầu: %s", a.getItem().getDescription(), bidder),
                bidder
        );
    }
}
