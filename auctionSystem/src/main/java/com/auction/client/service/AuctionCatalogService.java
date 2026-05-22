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
  private final NumberFormat currencyFormat = NumberFormat.getCurrencyInstance(Locale.of("vi", "VN"));

  public List<String> getAvailableStatuses() {
    return List.of("Tất cả", "OPEN", "RUNNING", "FINISHED", "PAID", "CANCELED");
  }

  /**
   * Lọc danh sách các phiên đấu giá theo từ khóa và trạng thái.
   *
   * @param keyword Từ khóa tìm kiếm.
   * @param status  Trạng thái cần lọc.
   * @return Danh sách các hàng đấu giá đã lọc.
   */
  public List<AuctionRow> filterAuctions(String keyword, String status) {
    String kw = (keyword == null) ? "" : keyword.toLowerCase().trim();
    boolean isFilterAll = (status == null || "Tất cả".equals(status));

    List<AuctionRow> result = new ArrayList<>();
    //Gọi Server để lấy danh sách thời gian thực
    List<Auction> onlineAuctions = fetchOnlineAuctions();

    for (Auction a : onlineAuctions) {
      if (matchesKeyword(a, kw) && (isFilterAll || a.getStatus().name().equalsIgnoreCase(status))) {
        result.add(toRow(a));
      }
    }
    return result;
  }

  // Hàm nội bộ gọi mạng
  @SuppressWarnings("unchecked")
  private List<Auction> fetchOnlineAuctions() {
    try {
      com.auction.client.network.SocketClient.getInstance().sendRequest(new com.auction.shared.network.GetAllAuctionsRequest());
      Object rawResponse = com.auction.client.network.SocketClient.getInstance().receiveResponse();

      if (rawResponse instanceof com.auction.shared.network.ServiceResult<?> response) {
        if (response.success() && response.data() instanceof List<?> list) {
          return (List<Auction>) list;
        }
      }
    } catch (Exception e) {
      System.err.println("Lỗi tải danh sách Sảnh: " + e.getMessage());
    }
    // Fallback: Nếu rớt mạng thì mới dùng danh sách lưu tạm dưới máy
    return com.auction.client.service.AuctionDataStore.getAuctions();
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
