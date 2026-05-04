package com.auction.client.service;

import com.auction.client.model.AuctionRoomViewModel;
import com.auction.shared.models.Auction;
import com.auction.shared.models.AuthUser;
import com.auction.shared.models.BidTransaction;
import com.auction.shared.models.Bidder;
import com.auction.shared.network.ServiceResult;
import java.text.NumberFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.Optional;

/**
 * Service xử lý logic cho phòng đấu giá tại Client.
 */
public class AuctionRoomService {
  private final NumberFormat cf = NumberFormat.getCurrencyInstance(Locale.of("vi", "VN"));
  private final DateTimeFormatter df = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

  public Optional<ServiceResult<AuctionRoomViewModel>> getAuctionRoom(String auctionId) {
    return AuctionDataStore.findById(auctionId).map(auction ->
        new ServiceResult<>(true, "", convert(auction)));
  }

  /**
   * Thực hiện đặt giá thầu từ Client.
   *
   * @param auctionId ID phiên đấu giá.
   * @param amountStr Số tiền thầu dưới dạng chuỗi.
   * @return Kết quả đặt thầu.
   */
  public ServiceResult<AuctionRoomViewModel> placeBid(String auctionId, String amountStr) {
    return AuctionDataStore.findById(auctionId).map(auction -> {
      try {
        String cleanAmount = amountStr.replaceAll("\\D", "");
        if (cleanAmount.isEmpty()) {
          return new ServiceResult<>(false, "Nhập số tiền hợp lệ.", convert(auction));
        }
        double amount = Double.parseDouble(cleanAmount);
        if (!auction.validateBid(amount)) {
          return new ServiceResult<>(false, "Giá thấp. Min: "
              + cf.format(auction.getCurrentPrice() + auction.getStepPrice()), convert(auction));
        }
        AuthUser currentUser = SessionContext.getCurrentUser();
        Bidder bidder = new Bidder(currentUser != null ? currentUser.getUsername() : "khách",
            "secret", amount * 2);
        auction.updateAuctionState(bidder, amount,
            new BidTransaction("TX-" + auctionId + "-" + (auction.getBidHistory().size() + 1),
                bidder, amount, LocalDateTime.now()));
        return new ServiceResult<>(true, "Thành công!", convert(auction));
      } catch (Exception e) {
        return new ServiceResult<>(false, "Lỗi: " + e.getMessage(), convert(auction));
      }
    }).orElse(new ServiceResult<>(false, "Không thấy phiên.", null));
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
