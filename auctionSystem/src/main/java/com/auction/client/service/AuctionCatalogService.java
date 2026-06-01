package com.auction.client.service;

import com.auction.client.network.SocketClient;
import com.auction.shared.models.auction.Auction;
import com.auction.shared.models.auction.AuctionRow;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import com.auction.shared.network.requests.ApproveAuctionRequest;
import com.auction.shared.network.requests.CancelAuctionRequest;
import com.auction.shared.network.requests.GetAllAuctionsRequest;
import com.auction.shared.network.requests.GetPendingAuctionsRequest;
import com.auction.shared.network.responses.ServiceResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Service xử lý danh mục đấu giá, hỗ trợ lọc và định dạng dữ liệu cho TableView.
 */
public class AuctionCatalogService {
  private static final Logger logger = LoggerFactory.getLogger(AuctionCatalogService.class);

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
    // Gọi Server để lấy danh sách thời gian thực
    List<Auction> onlineAuctions = fetchOnlineAuctions();

    for (Auction a : onlineAuctions) {
      if (matchesKeyword(a, kw) && (isFilterAll || a.getStatus().name().equalsIgnoreCase(status))) {
        result.add(toRow(a));
      }
    }
    return result;
  }

  // Hàm nội bộ gọi mạng
  private List<Auction> fetchOnlineAuctions() {
    try {
      SocketClient.getInstance().sendRequest(new GetAllAuctionsRequest());

      ServiceResult<List<Auction>> response = (ServiceResult<List<Auction>>) SocketClient.getInstance().receiveResponse();

      return response.data();

    } catch (Exception e) {
      logger.error("Lỗi tải danh sách Sảnh", e);
      // Fallback: Nếu rớt mạng thì dùng danh sách lưu tạm dưới máy
      return AuctionDataStore.getAuctions();
    }
  }

  /**
   * Lấy danh sách các phiên đấu giá đang chờ duyệt. (ĐÃ SỬA ÉP KIỂU AN TOÀN)
   */
  public List<AuctionRow> getPendingAuctions() {
    try {
      SocketClient.getInstance().sendRequest(new GetPendingAuctionsRequest());
      Object rawResponse = SocketClient.getInstance().receiveResponse();

      if (!(rawResponse instanceof ServiceResult<?> response)) {
        logger.warn("Kiểu dữ liệu phản hồi không hợp lệ khi tải danh sách chờ duyệt.");
        return new ArrayList<>();
      }

      if (!response.success()) {
        logger.warn("Lỗi từ Server (Danh sách chờ duyệt): {}", response.message());
        return new ArrayList<>();
      }

      if (!(response.data() instanceof List<?> rawList)) {
        logger.warn("Dữ liệu trả về không phải là danh sách (chờ duyệt).");
        return new ArrayList<>();
      }

      // Ép kiểu an toàn và map sang AuctionRow
      return rawList.stream()
              .filter(item -> item instanceof Auction)
              .map(item -> toRow((Auction) item))
              .toList();

    } catch (Exception e) {
      logger.error("Lỗi tải danh sách chờ duyệt", e);
    }
    return new ArrayList<>();
  }

  /**
   * Gửi yêu cầu phê duyệt phiên đấu giá lên server.
   */
  @SuppressWarnings("unchecked")
  public ServiceResult<Void> approveAuction(String auctionId) {
    try {
      SocketClient.getInstance().sendRequest(new ApproveAuctionRequest(auctionId));
      return (ServiceResult<Void>) SocketClient.getInstance().receiveResponse();
    } catch (Exception e) {
      return new ServiceResult<>(false, "Lỗi kết nối khi duyệt: " + e.getMessage(), null);
    }
  }

  /**
   * Gửi yêu cầu hủy phiên đấu giá lên server.
   *
   * @param auctionId ID của phiên đấu giá cần hủy.
   * @return Kết quả từ server.
   */
  @SuppressWarnings("unchecked")
  public ServiceResult<Void> cancelAuction(String auctionId) {
    try {
      SocketClient.getInstance().sendRequest(new CancelAuctionRequest(auctionId));
      return (ServiceResult<Void>) SocketClient.getInstance().receiveResponse();
    } catch (Exception e) {
      return new ServiceResult<>(false, "Lỗi kết nối khi hủy: " + e.getMessage(), null);
    }
  }

  private boolean matchesKeyword(Auction a, String kw) {
    if (kw.isEmpty()) {
      return true;
    }
    return a.getAuctionId().toLowerCase().contains(kw)
            || a.getItem().getName().toLowerCase().contains(kw);
  }

  private AuctionRow toRow(Auction a) {
    // LẤY TÊN THẬT NGƯỜI DẪN ĐẦU
    String topBidderName = "Chưa có";
    if (a.getHighestBidder() != null) {
      topBidderName = a.getHighestBidder().getFullName();
      if (topBidderName == null || topBidderName.trim().isEmpty()) {
        topBidderName = a.getHighestBidder().getUsername();
      }
    }

    // LẤY TÊN THẬT NGƯỜI BÁN
    String sellerName = "Ẩn danh";
    if (a.getSeller() != null) {
      sellerName = a.getSeller().getFullName();
      if (sellerName == null || sellerName.trim().isEmpty()) {
        sellerName = a.getSeller().getUsername();
      }
    }

    // ĐÓNG GÓI THÀNH HÀNG DỮ LIỆU ĐÃ TÁCH CỘT
    return new AuctionRow(
            a.getAuctionId(),
            a.getItem().getName(),
            sellerName,
            topBidderName,
            currencyFormat.format(a.getCurrentPrice()),
            currencyFormat.format(a.getStepPrice()),
            a.getStatus().name(),
            a.getItem().getDescription()
    );
  }
}