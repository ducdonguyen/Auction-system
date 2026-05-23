package com.auction.server.concurrency;

import com.auction.server.core.AuctionManager;
import com.auction.server.core.AuctionService;
import com.auction.server.dao.UserDao;
import com.auction.server.util.PasswordUtil;
import com.auction.shared.models.Auction;
import com.auction.shared.models.AuthUser;
import com.auction.shared.network.BidRequest;
import com.auction.shared.network.GetAllAuctionsRequest;
import com.auction.shared.network.JoinRoomRequest;
import com.auction.shared.network.LoginRequest;
import com.auction.shared.network.RegistrationRequest;
import com.auction.shared.network.ServiceResult;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.sql.SQLException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Lớp định tuyến các yêu cầu từ Client đến các phương thức xử lý tương ứng.
 */
public class RequestRouter {
  private RequestRouter() {
  }

  private static final Logger logger = LoggerFactory.getLogger(RequestRouter.class);
  private static final UserDao USER_DAO = new UserDao();

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
        default -> logger.warn("Unknown request: {}", request.getClass().getName());
      }
    } catch (Exception e) {
      logger.error("[RequestRouter] Error routing request: {}", e.getMessage(), e);
    }
  }

  private static void handleLogin(LoginRequest request, ObjectOutputStream out)
      throws SQLException, IOException {
    AuthUser user = USER_DAO.findByUsername(request.username());
    ServiceResult<AuthUser> result;
    if (user != null && PasswordUtil.matches(request.password(), user.getPasswordHash())) {
      result = new ServiceResult<>(true, "Login successful", user);
    } else {
      result = new ServiceResult<>(false, "Invalid username or password", null);
    }
    sendResponse(out, result);
  }

  private static void handleRegister(RegistrationRequest request, ObjectOutputStream out)
      throws SQLException, IOException {
    ServiceResult<AuthUser> result;
    if (USER_DAO.existsByUsernameOrEmail(request.username(), request.email())) {
      result = new ServiceResult<>(false, "Username or email already exists", null);
    } else {
      AuthUser newUser = new AuthUser(request.fullName(), request.username(), request.email(),
          PasswordUtil.hashPassword(request.password()), "BIDDER");
      USER_DAO.register(newUser);
      result = new ServiceResult<>(true, "Registration successful", newUser);
    }
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

  private static void sendResponse(ObjectOutputStream out, Object response) throws IOException {
    out.writeObject(response);
    out.flush();
  }
}
