package com.auction.client.service;

import com.auction.client.model.AuctionRoomViewModel;
import com.auction.shared.exceptions.AuctionClosedException;
import com.auction.shared.exceptions.InvalidBidException;
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
 * Service xử lý logic nghiệp vụ trong phòng đấu giá.
 */
public class AuctionRoomService {
    private final NumberFormat currencyFormatter = NumberFormat.getCurrencyInstance(new Locale("vi", "VN"));
    private final DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    public Optional<ServiceResult<AuctionRoomViewModel>> getAuctionRoom(String id) {
        return AuctionDataStore.findById(id).map(auction -> new ServiceResult<>(true, "", convertToViewModel(auction)));
    }

    public ServiceResult<AuctionRoomViewModel> placeBid(String auctionId, String amountText) {
        return AuctionDataStore.findById(auctionId).map(auction -> {
            try {
                // Normalize amount (remove non-digits)
                String cleanAmount = amountText.replaceAll("[^\\d]", "");
                if (cleanAmount.isEmpty()) {
                    return new ServiceResult<AuctionRoomViewModel>(false, "Vui lòng nhập số tiền hợp lệ.",
                            convertToViewModel(auction));
                }

                double bidAmount = Double.parseDouble(cleanAmount);

                // Check bid logic from Auction class
                if (!auction.validateBid(bidAmount)) {
                    String minBidStr = currencyFormatter.format(auction.getCurrentPrice() + auction.getStepPrice());
                    return new ServiceResult<AuctionRoomViewModel>(false, "Giá đặt phải ít nhất bằng: " + minBidStr,
                            convertToViewModel(auction));
                }

                // Get current bidder info
                AuthUser user = SessionContext.getCurrentUser();
                String bidderName = (user != null) ? user.getUsername() : "khách_vãng_lai";

                Bidder bidder = new Bidder(bidderName, "secret", bidAmount * 2);
                BidTransaction transaction = new BidTransaction(
                        "TX-" + auctionId + "-" + (auction.getBidHistory().size() + 1),
                        bidder,
                        bidAmount,
                        LocalDateTime.now()
                );

                // Update auction state
                auction.updateAuctionState(bidder, bidAmount, transaction);

                return new ServiceResult<>(true, "Đặt giá thành công cho phiên " + auctionId + "!",
                        convertToViewModel(auction));
            } catch (InvalidBidException e) {
                return new ServiceResult<AuctionRoomViewModel>(false, "Số tiền đấu giá không hợp lệ.",
                        convertToViewModel(auction));
            } catch (AuctionClosedException e) {
                return new ServiceResult<AuctionRoomViewModel>(false, "Phiên đấu giá đã kết thúc.",
                        convertToViewModel(auction));
            }
        }).orElse(new ServiceResult<>(false, "Không tìm thấy phiên đấu giá.", null));
    }

    private AuctionRoomViewModel convertToViewModel(Auction auction) {
        String leadingBidder = (auction.getHighestBidder() == null) ? "Chưa có người dẫn đầu" :
                auction.getHighestBidder().getUsername();
        double nextMinBid = auction.getCurrentPrice() + auction.getStepPrice();

        return new AuctionRoomViewModel(
                auction.getAuctionId(),
                auction.getItem().getName(),
                auction.getSeller().getUsername(),
                auction.getStatus().name(),
                currencyFormatter.format(auction.getCurrentPrice()),
                currencyFormatter.format(auction.getStepPrice()),
                currencyFormatter.format(nextMinBid),
                leadingBidder,
                auction.getItem().getDescription(),
                "Lịch phiên: " + dateTimeFormatter.format(auction.getStartTime()) + " - " +
                        dateTimeFormatter.format(auction.getEndTime()),
                auction.getBidHistory().stream()
                        .map(t -> t.getBidder().getUsername() + " đã đặt " +
                                currencyFormatter.format(t.getBidAmount()) + " lúc " +
                                dateTimeFormatter.format(t.getTimestamp()))
                        .toList()
        );
    }
}
