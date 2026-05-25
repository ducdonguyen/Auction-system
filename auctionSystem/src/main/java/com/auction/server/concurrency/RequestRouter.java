package com.auction.server.concurrency;

import com.auction.shared.network.CreateAuctionRequest;
import com.auction.server.core.AuctionManager;
import com.auction.server.core.AuctionService;
import com.auction.server.service.AuthService;
import com.auction.shared.models.Auction;
import com.auction.shared.models.AuthUser;
import com.auction.shared.models.ItemFactory;
import com.auction.shared.models.Item;
import com.auction.shared.models.Seller;
import com.auction.shared.network.BidRequest;
import com.auction.shared.network.CancelAuctionRequest;
import com.auction.shared.network.CreateAuctionRequest;
import com.auction.shared.network.GetPendingAuctionsRequest;
import com.auction.shared.network.ApproveAuctionRequest;
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
                case LoginRequest login -> handleLogin(login, handler, out);
                case BidRequest bid -> handleBid(bid, out, auctionService);
                case JoinRoomRequest join -> handleJoinRoom(join, handler, out, auctionService);
                case RegistrationRequest register -> handleRegister(register, out);
                case GetAllAuctionsRequest getAll -> handleGetAllAuctions(out, auctionService);
                case CancelAuctionRequest cancel -> handleCancelAuction(cancel, out, auctionService);
                case CreateAuctionRequest createReq -> handleCreateAuction(createReq, handler, out, auctionService);
                case GetPendingAuctionsRequest getPending -> handleGetPendingAuctions(out, auctionService);
                case ApproveAuctionRequest approve -> handleApproveAuction(approve, out, auctionService);

                default -> logger.warn("Unknown request: {}", request.getClass().getName());
            }
        } catch (Exception e) {
            logger.error("[RequestRouter] Error routing request: {}", e.getMessage(), e);
        }
    }

    private static void handleLogin(LoginRequest request, ClientHandler handler, ObjectOutputStream out)
            throws IOException {
        // Gọi thẳng sang AuthService để xử lý
        ServiceResult<AuthUser> result = AUTH_SERVICE.login(request);

        // Nếu đăng nhập thành công, đưa User này vào Sảnh (Global)
        if (result.success()) {
            AuctionManager.getInstance().addGlobalObserver(handler);
            logger.info("[RequestRouter] User {} đã đăng nhập và được thêm vào Global Observers", request.username());
        }

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
            //Hứng giá trị boolean để biết đặt giá thành công hay thất bại thực tế
            boolean isSuccess = auctionService.placeBid(request.getAuctionId(), request.getBidderName(), request.getAmount());

            if (isSuccess) {
                result = new ServiceResult<>(true, "Đặt giá thầu thành công!", null);
            } else {
                result = new ServiceResult<>(false, "Đặt giá thất bại! Vui lòng kiểm tra lại số tiền đặt (phải lớn hơn Giá hiện tại + Bước giá) hoặc trạng thái của phiên đấu giá.", null);
            }
        } catch (Exception e) {
            result = new ServiceResult<>(false, "Lỗi xử lý hệ thống: " + e.getMessage(), null);
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
            // 1. Lấy đúng tên người bán từ gói tin Client gửi lên
            String sellerUsername = request.getSellerUsername();
            if (sellerUsername == null || sellerUsername.isBlank()) {
                sellerUsername = "Người bán ẩn danh";
            }

            // 2. Dùng ItemFactory để đúc ra sản phẩm tương ứng (kể cả loại OTHER)
            Item item = ItemFactory.createItem(
                    request.getProductType(),
                    request.getProductName(),
                    request.getDescription(),
                    request.getStartingPrice(),
                    request.getExtraInfo()
            );

            Seller seller = new Seller(sellerUsername, "");

            // 3. Gọi hàm tạo phòng gốc của AuctionService
            Auction newAuction = auctionService.createAuction(
                    item, seller, request.getStartingPrice(), request.getPriceStep(),
                    request.getStartTime(), request.getEndTime()
            );

            if (newAuction != null) {
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

    /**
     * Xử lý yêu cầu lấy danh sách phiên đấu giá chờ duyệt (Dành cho Admin)
     */
    private static void handleGetPendingAuctions(ObjectOutputStream out, AuctionService auctionService) throws IOException {
        try {
            java.util.List<Auction> pendingList = auctionService.getPendingAuctions();
            sendResponse(out, new ServiceResult<>(true, "Lấy danh sách chờ duyệt thành công", pendingList));
        } catch (Exception e) {
            sendResponse(out, new ServiceResult<>(false, "Lỗi lấy danh sách: " + e.getMessage(), null));
        }
    }

    /**
     * Xử lý yêu cầu phê duyệt phiên đấu giá (Dành cho Admin)
     */
    private static void handleApproveAuction(ApproveAuctionRequest req, ObjectOutputStream out,
                                             AuctionService auctionService) throws IOException {
        try {
            Auction auction = auctionService.getAuctionById(req.getAuctionId());
            if (auction == null) {
                sendResponse(out, new ServiceResult<>(false, "Không tìm thấy ID phiên đấu giá", null));
                return;
            }

            // Gọi Core đổi trạng thái từ PENDING sang OPEN
            boolean success = auctionService.updateAuctionStatus(auction, com.auction.shared.models.AuctionStatus.OPEN);

            if (success) {
                sendResponse(out, new ServiceResult<>(true, "Đã duyệt thành công! Phiên đấu giá đã hiện trên Sảnh.", null));
            } else {
                sendResponse(out, new ServiceResult<>(false, "Trạng thái hiện tại không cho phép duyệt.", null));
            }
        } catch (Exception e) {
            sendResponse(out, new ServiceResult<>(false, "Lỗi duyệt phiên: " + e.getMessage(), null));
        }
    }

    private static void sendResponse(ObjectOutputStream out, Object response) throws IOException {
        out.writeObject(response);
        out.flush();
    }
}
