package com.auction.client.service;

import com.auction.client.model.AuctionRoomViewModel;
import com.auction.client.network.SocketClient;
import com.auction.shared.models.auction.Auction;
import com.auction.shared.models.auth.UserAccount;
import com.auction.shared.network.requests.BidRequest;
import com.auction.shared.network.requests.JoinRoomRequest;
import com.auction.shared.network.responses.ServiceResult;
import java.text.NumberFormat;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Service xử lý logic cho phòng đấu giá tại Client.
 */
public class AuctionRoomService {
    private static final Logger logger = LoggerFactory.getLogger(AuctionRoomService.class);
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
                    AuctionRoomViewModel vm = convert(latestAuction, response.serverTimeMillis());
                    return Optional.of(new ServiceResult<>(true, response.message(), vm, response.serverTimeMillis()));
                }
            }
        } catch (Exception e) {
            logger.error("Lỗi khi lấy thông tin phòng đấu giá ID: {}", auctionId, e);
        }

        // Fallback: Nếu rớt mạng hoặc có lỗi
        return AuctionDataStore.findById(auctionId).map(auction ->
                new ServiceResult<>(false, "Đang xem offline (Lỗi mạng)", convert(auction, System.currentTimeMillis()), System.currentTimeMillis()));
    }

    /**
     * Gửi gói tin đặt giá thầu lên Server.
     */
    public ServiceResult<AuctionRoomViewModel> placeBid(String auctionId, String amountStr) {
        try {
            // 1. Làm sạch chuỗi và kiểm tra định dạng số ngay tại Client
            String cleanAmount = amountStr.replaceAll("\\D", "");
            if (cleanAmount.isEmpty()) {
                return new ServiceResult<>(false, "Vui lòng nhập số tiền hợp lệ!", null, System.currentTimeMillis());
            }
            double amount = Double.parseDouble(cleanAmount);
            if (amount <= 0) {
                return new ServiceResult<>(false, "Số tiền đặt giá phải lớn hơn 0!", null, System.currentTimeMillis());
            }

            UserAccount currentUser = SessionContext.getCurrentUser();
            if (currentUser == null) {
                return new ServiceResult<>(false, "Bạn cần đăng nhập để đặt giá.", null, System.currentTimeMillis());
            }

            // 2. Tạo gói tin đặt giá
            BidRequest bidRequest = new BidRequest(auctionId, currentUser.getUsername(), amount);

            // 3. Gửi lệnh lên Server
            SocketClient.getInstance().sendRequest(bidRequest);

            // 4. Chờ và lấy kết quả từ Server
            Object rawResponse = SocketClient.getInstance().receiveResponse();

            // 5. Trả về đúng trạng thái và thông điệp thực tế của Server
            if (rawResponse instanceof ServiceResult<?> response) {
                long serverTimeMillis = response.serverTimeMillis();
                Object data = response.data();
                if (data instanceof com.auction.shared.models.auction.Auction updatedAuction) {
                    AuctionRoomViewModel vm = convert(updatedAuction, serverTimeMillis);
                    return new ServiceResult<>(response.success(), response.message(), vm, serverTimeMillis);
                }
                return new ServiceResult<>(response.success(), response.message(), null, serverTimeMillis);
            }

            return new ServiceResult<>(false, "Phản hồi từ Server không hợp lệ", null, System.currentTimeMillis());

        } catch (Exception e) {
            return new ServiceResult<>(false, "Lỗi kết nối mạng: " + e.getMessage(), null, System.currentTimeMillis());
        }
    }

    private AuctionRoomViewModel convert(Auction auction, long serverTimeMillis) {
        // Chuyển đổi và định dạng danh sách lịch sử đặt giá từ DB
        java.util.List<String> history = new java.util.ArrayList<>(auction.getBidHistory().stream()
                .map(tx -> {
                    String displayName = tx.bidder().getFullName();
                    if (displayName == null || displayName.trim().isEmpty()) {
                        displayName = tx.bidder().getUsername(); // Fallback nếu chưa có tên thật
                    }
                    return displayName + " đặt " + cf.format(tx.bidAmount()) + " lúc " + df.format(tx.timestamp());
                })
                .toList());

        // ĐẢO NGƯỢC DANH SÁCH: Đưa các giao dịch mới nhất lên vị trí đầu tiên (Index 0)
        java.util.Collections.reverse(history);

        // ========================================================
        // XỬ LÝ LOẠI SẢN PHẨM VÀ THÔNG TIN BỔ SUNG ĐỒNG BỘ VỚI FORM TẠO PHÒNG
        // ========================================================
        String rawType = auction.getItem().getItemType();
        String rawExtra = auction.getItem().getExtraInfo();

        // Dịch mã loại sản phẩm sang Tiếng Việt
        String vietnameseType = switch (rawType) {
            case "ELECTRONICS" -> "Điện tử";
            case "ART" -> "Tác phẩm nghệ thuật";
            case "VEHICLE" -> "Xe cộ";
            default -> "Khác";
        };

        // Định dạng thông tin chi tiết BÁM SÁT câu chữ trong AuctionListController
        String formattedExtra = switch (rawType) {
            case "ELECTRONICS" -> "Bảo hành (tháng): " + rawExtra;
            case "ART" -> "Tên tác giả (hoặc nhà sản xuất): " + rawExtra;
            case "VEHICLE" -> "Hãng xe: " + rawExtra;
            default -> "Chi tiết loại sản phẩm: " + rawExtra;
        };

        String formattedSchedule = "--/--/---- --:--";
        long endTimeMillis = 0;
        if (auction.getStartTime() != null && auction.getEndTime() != null) {
            formattedSchedule = "Lịch: " + df.format(auction.getStartTime()) + " - " + df.format(auction.getEndTime());
            long endTimeMillisServer = auction.getEndTime().atZone(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli();
            
            // Điều chỉnh bằng chênh lệch client-server: 
            // endTimeMillis = endTimeServer + (clientNow - serverNow)
            long clientNow = System.currentTimeMillis();
            long delta = clientNow - serverTimeMillis;
            endTimeMillis = endTimeMillisServer + delta;
        }

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
                formattedSchedule,
                history,
                vietnameseType,
                formattedExtra,
                endTimeMillis
        );
    }
}
