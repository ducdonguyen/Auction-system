package com.auction.client.service;

import com.auction.client.model.AuctionRoomViewModel;
import com.auction.client.network.SocketClient;
import com.auction.shared.models.Auction;
import com.auction.shared.models.AuthUser;
import com.auction.shared.network.BidRequest;
import com.auction.shared.network.JoinRoomRequest;
import com.auction.shared.network.ServiceResult;
import java.text.NumberFormat;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.Optional;

/**
 * Service xử lý logic cho phòng đấu giá tại Client.
 */
public class AuctionRoomService {
  private final NumberFormat cf = NumberFormat.getCurrencyInstance(Locale.of("vi", "VN"));
  private final DateTimeFormatter df = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

  /**
   * Lấy thông tin phòng đấu giá MỚI NHẤT từ Server.
   */
  public Optional<ServiceResult<AuctionRoomViewModel>> getAuctionRoom(String auctionId) {
    try {
      // 1. Tạo gói tin xin vào phòng
      JoinRoomRequest request = new JoinRoomRequest(auctionId);

      // 2. Gửi lệnh lên Server
      SocketClient.getInstance().sendRequest(request);

      // 3. Chờ và lấy kết quả từ BlockingQueue của SocketClient
      Object rawResponse = SocketClient.getInstance().receiveResponse();

      // 4. Kiểm tra và ép kiểu
      if (rawResponse instanceof ServiceResult<?> response) {
        if (response.success() && response.data() instanceof Auction latestAuction) {
          return Optional.of(new ServiceResult<>(true, response.message(), convert(latestAuction)));
        }
      }
    } catch (Exception e) {
      e.printStackTrace();
    }

    // Fallback: Nếu rớt mạng hoặc có lỗi
    return AuctionDataStore.findById(auctionId).map(auction ->
            new ServiceResult<>(false, "Đang xem offline (Lỗi mạng)", convert(auction)));
  }

  /**
   * Gửi gói tin đặt giá thầu lên Server.
   */
  public ServiceResult<AuctionRoomViewModel> placeBid(String auctionId, String amountStr) {
    try {
      String cleanAmount = amountStr.replaceAll("\\D", "");
      if (cleanAmount.isEmpty()) {
        return new ServiceResult<>(false, "Vui lòng nhập số tiền hợp lệ.", null);
      }
      double amount = Double.parseDouble(cleanAmount);

      AuthUser currentUser = SessionContext.getCurrentUser();
      if (currentUser == null) {
        return new ServiceResult<>(false, "Bạn cần đăng nhập để đặt giá.", null);
      }

      // 1. Tạo gói tin đặt giá
      BidRequest bidRequest = new BidRequest(auctionId, currentUser.getUsername(), amount);

      // 2. Gửi lệnh lên Server
      SocketClient.getInstance().sendRequest(bidRequest);

      // 3. Chờ và lấy kết quả từ BlockingQueue
      Object rawResponse = SocketClient.getInstance().receiveResponse();

      // 4. Ép kiểu và đọc thông báo từ Server
      if (rawResponse instanceof ServiceResult<?> response) {
        if (response.success()) {
          return new ServiceResult<>(true, "Đã gửi lệnh đặt giá!", null);
        } else {
          return new ServiceResult<>(false, response.message(), null);
        }
      }

      return new ServiceResult<>(false, "Phản hồi từ Server không hợp lệ", null);

    } catch (Exception e) {
      return new ServiceResult<>(false, "Lỗi kết nối mạng: " + e.getMessage(), null);
    }
  }

  private AuctionRoomViewModel convert(Auction auction) {
    return new AuctionRoomViewModel(
            auction.getAuctionId(),
            auction.getItem().getName(),
            auction.getSeller().getUsername(),
            auction.getStatus().name(),
            cf.format(auction.getCurrentPrice()),
            cf.format(auction.getStepPrice()),
            cf.format(auction.getCurrentPrice() + auction.getStepPrice()),
            auction.getHighestBidder() == null ? "Chưa có" : auction.getHighestBidder().getUsername(),
            auction.getItem().getDescription(),
            "Lịch: " + df.format(auction.getStartTime()) + " - " + df.format(auction.getEndTime()),
            auction.getBidHistory().stream()
                    .map(tx -> tx.bidder().getUsername() + " đặt " + cf.format(tx.bidAmount())
                            + " lúc " + df.format(tx.timestamp()))
                    .toList());
  }
}