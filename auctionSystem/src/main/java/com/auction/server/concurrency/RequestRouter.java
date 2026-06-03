package com.auction.server.concurrency;

import com.auction.shared.models.auction.AuctionStatus;
import com.auction.shared.models.auction.BidTransaction;
import com.auction.shared.network.requests.CreateAuctionRequest;
import com.auction.server.core.AuctionManager;
import com.auction.server.core.AuctionService;
import com.auction.server.service.AuthService;
import com.auction.shared.models.auction.Auction;
import com.auction.shared.models.auth.UserAccount;
import com.auction.shared.models.item.ItemFactory;
import com.auction.shared.models.item.Item;
import com.auction.shared.models.auth.Seller;
import com.auction.shared.network.requests.BidRequest;
import com.auction.shared.network.requests.CancelAuctionRequest;
import com.auction.shared.network.requests.GetPendingAuctionsRequest;
import com.auction.shared.network.requests.ApproveAuctionRequest;
import com.auction.shared.network.requests.GetAllAuctionsRequest;
import com.auction.shared.network.requests.JoinRoomRequest;
import com.auction.shared.network.requests.LoginRequest;
import com.auction.shared.network.requests.RegistrationRequest;
import com.auction.shared.network.responses.ServiceResult;
import com.auction.shared.network.requests.TopUpRequest;   // IMPORT MỚI
import com.auction.shared.network.responses.TopUpResponse;  // IMPORT MỚI
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.time.LocalDateTime;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Lớp định tuyến các yêu cầu từ Client đến các phương thức xử lý tương ứng.
 */
public class RequestRouter {

    private static final Logger logger = LoggerFactory.getLogger(RequestRouter.class);

    // Các dependency được tiêm qua constructor thay vì hard-code
    private final AuthService authService;
    private final AuctionService auctionService;

    /**
     * Khởi tạo RequestRouter với các dịch vụ cần thiết.
     *
     * @param authService    Dịch vụ xác thực.
     * @param auctionService Dịch vụ đấu giá.
     */
    public RequestRouter(AuthService authService, AuctionService auctionService) {
        this.authService = authService;
        this.auctionService = auctionService;
    }

    /**
     * Định tuyến yêu cầu.
     *
     * @param request        Đối tượng yêu cầu.
     * @param handler        ClientHandler xử lý kết nối.
     * @param out            Luồng ghi phản hồi.
     */
    public void route(Object request, ClientHandler handler, ObjectOutputStream out) {
        try {
            switch (request) {
                case LoginRequest login -> handleLogin(login, handler, out);
                case BidRequest bid -> handleBid(bid, out);
                case JoinRoomRequest join -> handleJoinRoom(join, handler, out);
                case RegistrationRequest register -> handleRegister(register, out);
                case GetAllAuctionsRequest getAll -> handleGetAllAuctions(out);
                case CancelAuctionRequest cancel -> handleCancelAuction(cancel, out);
                case CreateAuctionRequest createReq -> handleCreateAuction(createReq, handler, out);
                case GetPendingAuctionsRequest getPending -> handleGetPendingAuctions(out);
                case ApproveAuctionRequest approve -> handleApproveAuction(approve, out);

                // NHÁNH MỚI: Bắt gói tin yêu cầu nạp tiền real-time
                case TopUpRequest topUpReq -> handleTopUp(topUpReq, out);

                default -> logger.warn("Unknown request: {}", request.getClass().getName());
            }
        } catch (Exception e) {
            logger.error("[RequestRouter] Error routing request: {}", e.getMessage(), e);
        }
    }

     private void handleLogin(LoginRequest request, ClientHandler handler, ObjectOutputStream out)
             throws IOException {
         ServiceResult<UserAccount> result = authService.login(request);
         // Thêm thời gian server vào response
         if (result.success()) {
             // Ghi danh user vào hệ thống để có thể nhận tin nhắn hoàn tiền
             handler.setUsername(request.username());

             AuctionManager.getInstance().addGlobalObserver(handler);
             logger.info("[RequestRouter] User {} đã đăng nhập và được thêm vào Global Observers", request.username());
             result = new ServiceResult<>(result.success(), result.message(), result.data(), System.currentTimeMillis());
         } else {
             result = new ServiceResult<>(result.success(), result.message(), result.data(), System.currentTimeMillis());
         }

         sendResponse(out, result);
     }

     private void handleRegister(RegistrationRequest request, ObjectOutputStream out)
             throws IOException {
         ServiceResult<UserAccount> result = authService.register(request);
         result = new ServiceResult<>(result.success(), result.message(), result.data(), System.currentTimeMillis());
         sendResponse(out, result);
     }

     private void handleJoinRoom(JoinRoomRequest request, ClientHandler handler,
                                 ObjectOutputStream out)
             throws IOException {
         String auctionId = request.getAuctionId();
         String oldAuctionId = handler.getCurrentWatchingAuctionId();
         if (oldAuctionId != null) {
             AuctionManager.getInstance().unsubscribe(oldAuctionId, handler);
         }
         handler.setCurrentWatchingAuctionId(auctionId);
         AuctionManager.getInstance().subscribe(auctionId, handler);
         Auction currentAuction = auctionService.getAuctionById(auctionId);

         if (currentAuction != null && currentAuction.getBidHistory() != null) {
             for (BidTransaction tx : currentAuction.getBidHistory()) {
                 if (tx.bidder() != null) {
                     tx.bidder().setFullName(authService.getFullName(tx.bidder().getUsername()));
                 }
             }
             if (currentAuction.getHighestBidder() != null) {
                 currentAuction.getHighestBidder().setFullName(authService.getFullName(currentAuction.getHighestBidder().getUsername()));
             }
         }

         sendResponse(out, new ServiceResult<>(true, "Joined room " + auctionId, currentAuction, System.currentTimeMillis()));
     }

    private void handleBid(BidRequest request, ObjectOutputStream out) throws IOException {
        ServiceResult<Void> result;
        try {
            boolean success = auctionService.placeBid(request.getAuctionId(), request.getBidderName(), request.getAmount());
            if (success) {
                result = new ServiceResult<>(true, "Đặt giá thầu thành công!", null, System.currentTimeMillis());
            } else {
                result = new ServiceResult<>(false, "Không thể đặt giá thầu.", null, System.currentTimeMillis());
            }
        } catch (IllegalArgumentException e) {
            result = new ServiceResult<>(false, e.getMessage(), null, System.currentTimeMillis());
        } catch (Exception e) {
            result = new ServiceResult<>(false, "Lỗi xử lý hệ thống: " + e.getMessage(), null, System.currentTimeMillis());
        }
        sendResponse(out, result);
    }

     private void handleGetAllAuctions(ObjectOutputStream out) throws IOException {
         java.util.List<Auction> allAuctions = auctionService.getAllAuctions();
         sendResponse(out, new ServiceResult<>(true, "Lấy danh sách thành công", allAuctions, System.currentTimeMillis()));
     }

     private void handleCancelAuction(CancelAuctionRequest request, ObjectOutputStream out) throws IOException {
         ServiceResult<Void> result;
         try {
             auctionService.cancelAuction(request.auctionId());
             result = new ServiceResult<>(true, "Đã hủy phiên đấu giá thành công", null, System.currentTimeMillis());
         } catch (Exception e) {
             result = new ServiceResult<>(false, "Lỗi hủy phiên: " + e.getMessage(), null, System.currentTimeMillis());
         }
         sendResponse(out, result);
     }

     /**
      * Xử lý yêu cầu tạo phiên đấu giá từ Client đưa xuống Service.
      */
     private void handleCreateAuction(CreateAuctionRequest request, ClientHandler handler,
                                      ObjectOutputStream out) throws IOException {
         ServiceResult<Void> result;
         try {
             String sellerUsername = request.getSellerUsername();
             if (sellerUsername == null || sellerUsername.isBlank()) {
                 sellerUsername = "Người bán ẩn danh";
             }

             Item item = ItemFactory.createItem(
                     request.getProductType(),
                     request.getProductName(),
                     request.getDescription(),
                     request.getStartingPrice(),
                     request.getExtraInfo()
             );

             Seller seller = new Seller(sellerUsername, "");

             // Tính startTime/endTime server-side nếu client gửi duration
             LocalDateTime startTime;
             LocalDateTime endTime;

             if (request.getDurationValue() != null && request.getDurationValue() > 0 && request.getDurationUnit() != null) {
                 startTime = LocalDateTime.now(); // dùng thời gian server để tránh bị gian lận
                 String unit = request.getDurationUnit();
                 long val = request.getDurationValue();

                 // ĐÃ SỬA: Chuyển chuỗi sang chữ thường ngay trong biểu thức switch
                 switch (unit.toLowerCase()) {
                     case "giờ":
                     case "hours":
                     case "hour":
                     case "h":
                         endTime = startTime.plusHours(val);
                         break;
                     case "phút":
                     case "minutes":
                     case "minute":
                     case "m":
                         endTime = startTime.plusMinutes(val);
                         break;
                     default: // "ngày" hoặc "days"
                         endTime = startTime.plusDays(val);
                         break;
                 }
             } else {
                 // fallback: dùng thời gian client gửi (còn giữ hành vi cũ)
                 startTime = request.getStartTime() != null ? request.getStartTime() : LocalDateTime.now();
                 endTime = request.getEndTime() != null ? request.getEndTime() : startTime.plusDays(3);
             }

             Auction newAuction = auctionService.createAuction(
                     item, seller, request.getStartingPrice(), request.getPriceStep(),
                     startTime, endTime
             );

             if (newAuction != null) {
                 result = new ServiceResult<>(true, "Đã tiếp nhận yêu cầu tạo phòng đấu giá, vui lòng chờ duyệt.", null, System.currentTimeMillis());
                 logger.info("[RequestRouter] Xử lý thành công CreateAuctionRequest cho sản phẩm: {}", request.getProductName());
             } else {
                 result = new ServiceResult<>(false, "Hệ thống không thể khởi tạo phiên đấu giá vào lúc này.", null, System.currentTimeMillis());
             }
         } catch (Exception e) {
             result = new ServiceResult<>(false, "Lỗi Server: " + e.getMessage(), null, System.currentTimeMillis());
             logger.error("[RequestRouter] Lỗi khi xử lý CreateAuctionRequest", e);
         }

         sendResponse(out, result);
     }

     /**
      * Xử lý yêu cầu lấy danh sách phiên đấu giá chờ duyệt (Dành cho Admin)
      */
     private void handleGetPendingAuctions(ObjectOutputStream out) throws IOException {
         try {
             java.util.List<Auction> pendingList = auctionService.getPendingAuctions();
             sendResponse(out, new ServiceResult<>(true, "Lấy danh sách chờ duyệt thành công", pendingList, System.currentTimeMillis()));
         } catch (Exception e) {
             sendResponse(out, new ServiceResult<>(false, "Lỗi lấy danh sách: " + e.getMessage(), null, System.currentTimeMillis()));
         }
     }

     /**
      * Xử lý yêu cầu phê duyệt phiên đấu giá (Dành cho Admin)
      */
     private void handleApproveAuction(ApproveAuctionRequest req, ObjectOutputStream out) throws IOException {
         try {
             Auction auction = auctionService.getAuctionById(req.getAuctionId());
             if (auction == null) {
                 sendResponse(out, new ServiceResult<>(false, "Không tìm thấy ID phiên đấu giá", null, System.currentTimeMillis()));
                 return;
             }

             boolean success = auctionService.updateAuctionStatus(auction, AuctionStatus.OPEN);

             if (success) {
                 sendResponse(out, new ServiceResult<>(true, "Đã duyệt thành công! Phiên đấu giá đã hiện trên Sảnh.", null, System.currentTimeMillis()));
             } else {
                 sendResponse(out, new ServiceResult<>(false, "Trạng thái hiện tại không cho phép duyệt.", null, System.currentTimeMillis()));
             }
         } catch (Exception e) {
             sendResponse(out, new ServiceResult<>(false, "Lỗi duyệt phiên: " + e.getMessage(), null, System.currentTimeMillis()));
         }
     }

    /**
     * HÀM MỚI THÊM VÀO: Tiếp nhận và điều hướng xử lý cộng tiền từ TopUpRequest
     */
    private void handleTopUp(TopUpRequest request, ObjectOutputStream out) throws IOException {
        TopUpResponse response;
        try {
            // Thực hiện nghiệp vụ nạp tiền thông qua AuthService của bạn
            double newBalance = authService.topUpBalance(request.getUserId(), request.getAmount());

            // Khởi tạo gói tin phản hồi thành công gửi lại Client
            response = new TopUpResponse(true, String.format("Đã cộng %,.0f đ vào tài khoản.", request.getAmount()), newBalance);
            logger.info("[SERVER] Nạp tiền thành công cho User {}: +{}", request.getUserId(), request.getAmount());
        } catch (Exception e) {
            response = new TopUpResponse(false, "Lỗi hệ thống khi nạp tiền: " + e.getMessage(), 0);
            logger.error("[SERVER] Thất bại khi nạp tiền cho User {}", request.getUserId(), e);
        }
        sendResponse(out, response);
    }

    private void sendResponse(ObjectOutputStream out, Object response) throws IOException {
        out.writeObject(response);
        out.flush();
    }
}