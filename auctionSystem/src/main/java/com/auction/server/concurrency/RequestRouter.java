package com.auction.server.concurrency;

import com.auction.shared.network.CreateAuctionRequest;
import com.auction.server.core.AuctionManager;
import com.auction.server.core.AuctionService;
import com.auction.server.service.AuthService;
import com.auction.shared.models.Auction;
import com.auction.shared.models.AuthUser;
import com.auction.shared.network.BidRequest;
import com.auction.shared.network.CancelAuctionRequest;
import com.auction.shared.network.CreateAuctionRequest;
import com.auction.shared.network.GetAllAuctionsRequest;
import com.auction.shared.network.JoinRoomRequest;
import com.auction.shared.network.LoginRequest;
import com.auction.shared.network.RegistrationRequest;
import com.auction.shared.network.ServiceResult;
import java.io.IOException;
import java.io.ObjectOutputStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Lớp định tuyến các yêu cầu từ Client đến các phương thức xử lý tương ứng.
 */
public class RequestRouter {
    private RequestRouter() {
    }

    private static final Logger logger = LoggerFactory.getLogger(RequestRouter.class);
    // Sử dụng AuthService từ phiên bản 1 thay cho UserDao
    private static final AuthService AUTH_SERVICE = new AuthService();

    /**
     * Định tuyến yêu cầu.
     *
     * @param request        Đối tượng yêu cầu.
     * @param handler        ClientHandler xử lý kết nối.
     * @param out            Luồng ghi phản hồi.
     * @param auctionService Dịch vụ đấu giá.
     */
    public static void route(Object request, ClientHandler handler, ObjectOutputStream out,
                             AuctionService auctionService) {
        try {
            switch (request) {
                case LoginRequest login -> handleLogin(login, out);
                case BidRequest bid -> handleBid(bid, out, auctionService);
                case JoinRoomRequest join -> handleJoinRoom(join, handler, out, auctionService);
                case RegistrationRequest register -> handleRegister(register, out);
                case GetAllAuctionsRequest getAll -> handleGetAllAuctions(out, auctionService);

                // Nhánh Hủy phiên đấu giá (Từ phiên bản 1)
                case CancelAuctionRequest cancel -> handleCancelAuction(cancel, out, auctionService);

                // Nhánh Tạo phiên đấu giá mới (Từ phiên bản 2)
                case CreateAuctionRequest createReq -> handleCreateAuction(createReq, handler, out, auctionService);

                default -> logger.warn("Unknown request: {}", request.getClass().getName());
            }
        } catch (Exception e) {
            logger.error("[RequestRouter] Error routing request: {}", e.getMessage(), e);
        }
    }

    private static void handleLogin(LoginRequest request, ObjectOutputStream out)
            throws IOException {
        // Gọi thẳng sang AuthService để xử lý
        ServiceResult<AuthUser> result = AUTH_SERVICE.login(request);
        sendResponse(out, result);
    }

    private static void handleRegister(RegistrationRequest request, ObjectOutputStream out)
            throws IOException {
        // Gọi thẳng sang AuthService để xử lý
        ServiceResult<AuthUser> result = AUTH_SERVICE.register(request);
        sendResponse(out, result);
    }

    private static void handleJoinRoom(JoinRoomRequest request, ClientHandler handler,
                                       ObjectOutputStream out, AuctionService auctionService)
            throws IOException {
        String auctionId = request.getAuctionId();
        // Unsubscribe from old room if any
        String oldAuctionId = handler.getCurrentWatchingAuctionId();
        if (oldAuctionId != null) {
            AuctionManager.getInstance().unsubscribe(oldAuctionId, handler);
        }
        handler.setCurrentWatchingAuctionId(auctionId);
        AuctionManager.getInstance().subscribe(auctionId, handler);
        Auction currentAuction = auctionService.getAuctionById(auctionId);
        sendResponse(out, new ServiceResult<>(true, "Joined room " + auctionId, currentAuction));
    }

    private static void handleBid(BidRequest request, ObjectOutputStream out,
                                  AuctionService auctionService)
            throws IOException {
        ServiceResult<Void> result;
        try {
            auctionService.placeBid(request.getAuctionId(), request.getBidderName(), request.getAmount());
            result = new ServiceResult<>(true, "Bid placed successfully", null);
        } catch (Exception e) {
            result = new ServiceResult<>(false, e.getMessage(), null);
        }
        sendResponse(out, result);
    }

    private static void handleGetAllAuctions(ObjectOutputStream out, AuctionService auctionService) throws IOException {
        java.util.List<Auction> allAuctions = auctionService.getAllAuctions();
        sendResponse(out, new ServiceResult<>(true, "Lấy danh sách thành công", allAuctions));
    }

    private static void handleCancelAuction(CancelAuctionRequest request, ObjectOutputStream out,
                                            AuctionService auctionService) throws IOException {
        ServiceResult<Void> result;
        try {
            // Gọi xuống AuctionService để thực hiện logic hủy (Đổi status CANCELED trong DB)
            auctionService.cancelAuction(request.auctionId());
            result = new ServiceResult<>(true, "Đã hủy phiên đấu giá thành công", null);
        } catch (Exception e) {
            result = new ServiceResult<>(false, "Lỗi hủy phiên: " + e.getMessage(), null);
        }
        sendResponse(out, result);
    }

    /**
     * Xử lý yêu cầu tạo phiên đấu giá từ Client đưa xuống Service.
     */
    private static void handleCreateAuction(CreateAuctionRequest request, ClientHandler handler,
                                            ObjectOutputStream out, AuctionService auctionService) throws IOException {
        ServiceResult<Void> result;
        try {
            String sellerUsername = "Người bán hệ thống";

            // Đẩy xuống tầng nghiệp vụ xử lý lưu Database dạng PENDING
            boolean success = auctionService.handleCreateAuctionRequest(request, sellerUsername);

            if (success) {
                result = new ServiceResult<>(true, "Đã tiếp nhận yêu cầu tạo phòng đấu giá, vui lòng chờ duyệt.", null);
                logger.info("[RequestRouter] Xử lý thành công CreateAuctionRequest cho sản phẩm: {}", request.getProductName());
            } else {
                result = new ServiceResult<>(false, "Hệ thống không thể khởi tạo phiên đấu giá vào lúc này.", null);
            }
        } catch (Exception e) {
            result = new ServiceResult<>(false, "Lỗi Server: " + e.getMessage(), null);
            logger.error("[RequestRouter] Lỗi khi xử lý CreateAuctionRequest", e);
        }
        // Gửi phản hồi trạng thái kỹ thuật về lại cho Client
        sendResponse(out, result);
    }

    private static void sendResponse(ObjectOutputStream out, Object response) throws IOException {
        out.writeObject(response);
        out.flush();
    }
}
