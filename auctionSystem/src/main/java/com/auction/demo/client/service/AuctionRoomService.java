package com.auction.demo.client.service;

import com.auction.demo.client.model.AuctionRoomViewModel;
import com.auction.demo.client.model.ServiceResult;
import com.auction.shared.models.*;
import java.text.NumberFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.Optional;

/**
 * Service xử lý logic nghiệp vụ trong phòng đấu giá.
 */
public class AuctionRoomService {
    private final NumberFormat cur = NumberFormat.getCurrencyInstance(new Locale("vi", "VN"));
    private final DateTimeFormatter df = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    public Optional<ServiceResult<AuctionRoomViewModel>> getAuctionRoom(String id) {
        return AuctionDataStore.findById(id).map(a -> new ServiceResult<>(true, "", toVM(a)));
    }

    public ServiceResult<AuctionRoomViewModel> placeBid(String id, String amtText) {
        return AuctionDataStore.findById(id).map(a -> {
            try {
                // Chuẩn hóa số tiền (loại bỏ ký tự không phải số)
                String cleanAmt = amtText.replaceAll("[^\\d]", "");
                if (cleanAmt.isEmpty()) return new ServiceResult<AuctionRoomViewModel>(false, "Vui lòng nhập số tiền hợp lệ.", toVM(a));
                
                double val = Double.parseDouble(cleanAmt);
                
                // Kiểm tra logic đặt giá từ lớp Auction
                if (!a.isValidBid(val)) {
                    return new ServiceResult<AuctionRoomViewModel>(false, "Giá đặt phải ít nhất bằng: " + cur.format(a.getCurrentPrice() + a.getStepPrice()), toVM(a));
                }

                // Lấy thông tin người đặt giá hiện tại
                AuthUser user = SessionContext.getCurrentUser();
                String name = (user != null) ? user.getUsername() : "khách_vãng_lai";
                
                Bidder bidder = new Bidder(name, "secret", val * 2);
                BidTransaction transaction = new BidTransaction(
                        "TX-" + id + "-" + (a.getBidHistory().size() + 1),
                        bidder,
                        val,
                        LocalDateTime.now()
                );

                // Cập nhật trạng thái phiên đấu giá
                a.updateAuctionState(bidder, val, transaction);
                
                return new ServiceResult<>(true, "Đặt giá thành công cho phiên " + id + "!", toVM(a));
            } catch (Exception e) { 
                return new ServiceResult<AuctionRoomViewModel>(false, "Lỗi xử lý: Số tiền không hợp lệ.", toVM(a)); 
            }
        }).orElse(new ServiceResult<>(false, "Không tìm thấy phiên đấu giá.", null));
    }

    private AuctionRoomViewModel toVM(Auction a) {
        String h = (a.getHighestBidder() == null) ? "Chưa có người dẫn đầu" : a.getHighestBidder().getUsername();
        return new AuctionRoomViewModel(
            a.getAuctionId(), 
            a.getItem().getName(), 
            a.getSeller().getUsername(), 
            a.getStatus().name(), 
            cur.format(a.getCurrentPrice()), 
            cur.format(a.getStepPrice()), 
            cur.format(a.getCurrentPrice() + a.getStepPrice()), 
            h, 
            a.getItem().getDescription(), 
            "Lịch phiên: " + df.format(a.getStartTime()) + " - " + df.format(a.getEndTime()), 
            a.getBidHistory().stream()
                .map(t -> t.getBidder().getUsername() + " đã đặt " + cur.format(t.getBidAmount()) + " lúc " + df.format(t.getTimestamp()))
                .toList()
        );
    }
}
