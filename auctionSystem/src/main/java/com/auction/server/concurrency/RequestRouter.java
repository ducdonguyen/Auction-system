package com.auction.server.concurrency;

import com.auction.server.core.AuctionManager;
import com.auction.server.core.AuctionService;
import com.auction.server.dao.UserDao;
import com.auction.shared.models.*;
import com.auction.shared.network.*;
import com.auction.client.util.PasswordUtil;

import java.io.ObjectOutputStream;
import java.sql.SQLException;

public class RequestRouter {
    private static final UserDao userDao = new UserDao();

    public static void route(Object request, ClientHandler handler, ObjectOutputStream out, AuctionService auctionService) {
        try {
            switch (request) {
                case LoginRequest login -> handleLogin(login, out);
                case BidRequest bid -> handleBid(bid, out, auctionService);
                case JoinRoomRequest join -> handleJoinRoom(join, handler, out);
                default -> System.out.println("Unknown request");
            }
        } catch (Exception e) {
            System.err.println("[RequestRouter] Error routing request: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private static void handleLogin(LoginRequest request, ObjectOutputStream out) throws Exception {
        AuthUser user = userDao.findByUsername(request.username());
        ServiceResult<AuthUser> result;
        if (user != null && PasswordUtil.matches(request.password(), user.getPasswordHash())) {
            result = new ServiceResult<>(true, "Login successful", user);
        } else {
            result = new ServiceResult<>(false, "Invalid username or password", null);
        }
        sendResponse(out, result);
    }

    private static void handleRegister(RegistrationRequest request, ObjectOutputStream out) throws Exception {
        ServiceResult<AuthUser> result;
        if (userDao.existsByUsernameOrEmail(request.username(), request.email())) {
            result = new ServiceResult<>(false, "Username or email already exists", null);
        } else {
            AuthUser newUser = new AuthUser(request.fullName(), request.username(), request.email(), PasswordUtil.hashPassword(request.password()));
            userDao.register(newUser);
            result = new ServiceResult<>(true, "Registration successful", newUser);
        }
        sendResponse(out, result);
    }

    private static void handleJoinRoom(JoinRoomRequest request, ClientHandler handler, ObjectOutputStream out) throws Exception {
        String auctionId = request.getAuctionId();
        // Unsubscribe from old room if any
        String oldAuctionId = handler.getCurrentWatchingAuctionId();
        if (oldAuctionId != null) {
            AuctionManager.getInstance().unsubscribe(oldAuctionId, handler);
        }
        handler.setCurrentWatchingAuctionId(auctionId);
        AuctionManager.getInstance().subscribe(auctionId, handler);
        sendResponse(out, new ServiceResult<>(true, "Joined room " + auctionId, null));
    }

    private static void handleBid(BidRequest request, ObjectOutputStream out, AuctionService auctionService) throws Exception {
        ServiceResult<Void> result;
        try {
            auctionService.placeBid(request.getAuctionId(), request.getBidderName(), request.getAmount());
            result = new ServiceResult<>(true, "Bid placed successfully", null);
        } catch (Exception e) {
            result = new ServiceResult<>(false, e.getMessage(), null);
        }
        sendResponse(out, result);
    }

    private static void sendResponse(ObjectOutputStream out, Object response) throws Exception {
        out.writeObject(response);
        out.flush();
    }
}
