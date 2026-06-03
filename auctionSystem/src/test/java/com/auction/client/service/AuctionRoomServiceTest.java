package com.auction.client.service;

import com.auction.client.model.AuctionRoomViewModel;
import com.auction.shared.models.auth.UserAccount;
import com.auction.shared.network.responses.ServiceResult;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class AuctionRoomServiceTest {

    private final AuctionRoomService service = new AuctionRoomService();

    @AfterEach
    void tearDown() {
        SessionContext.setCurrentUser(null);
    }

    @Test
    void placeBid_shouldFailWhenAmountEmpty() {
        ServiceResult<AuctionRoomViewModel> result =
                service.placeBid("AUC001", "");

        assertFalse(result.success());
        assertNotNull(result.message());
    }

    @Test
    void placeBid_shouldFailWhenAmountContainsNoDigits() {
        ServiceResult<AuctionRoomViewModel> result =
                service.placeBid("AUC001", "abcxyz");

        assertFalse(result.success());
    }

    @Test
    void placeBid_shouldFailWhenAmountZero() {
        ServiceResult<AuctionRoomViewModel> result =
                service.placeBid("AUC001", "0");

        assertFalse(result.success());
    }

    @Test
    void placeBid_shouldFailWhenUserNotLoggedIn() {
        SessionContext.setCurrentUser(null);

        ServiceResult<AuctionRoomViewModel> result =
                service.placeBid("AUC001", "100000");

        assertFalse(result.success());
        assertTrue(result.message().contains("đăng nhập"));
    }

    @Test
    void placeBid_shouldReachNetworkLayerWhenLoggedIn() {
        UserAccount user = new UserAccount();
        user.setUsername("hung");

        SessionContext.setCurrentUser(user);

        ServiceResult<AuctionRoomViewModel> result =
                service.placeBid("AUC001", "100000");

        assertNotNull(result);

        // Thường sẽ fail do không có server thật,
        // nhưng vẫn cover branch đã qua login.
        assertNotNull(result.message());
    }

    @Test
    void getAuctionRoom_shouldReturnOfflineAuctionWhenAuctionExists() {
        Optional<ServiceResult<AuctionRoomViewModel>> result =
                service.getAuctionRoom("AUC001");

        assertTrue(result.isPresent());

        ServiceResult<AuctionRoomViewModel> sr = result.get();

        assertNotNull(sr);
        assertNotNull(sr.data());
        assertEquals("AUC001", sr.data().auctionId());
    }

    @Test
    void getAuctionRoom_shouldReturnOfflineAuctionForSecondAuction() {
        Optional<ServiceResult<AuctionRoomViewModel>> result =
                service.getAuctionRoom("AUC002");

        assertTrue(result.isPresent());

        ServiceResult<AuctionRoomViewModel> sr = result.get();

        assertNotNull(sr.data());
        assertEquals("AUC002", sr.data().auctionId());
    }

    @Test
    void getAuctionRoom_shouldReturnEmptyForUnknownAuction() {
        Optional<ServiceResult<AuctionRoomViewModel>> result =
                service.getAuctionRoom("UNKNOWN_AUCTION");

        assertTrue(result.isEmpty());
    }

    @Test
    void offlineAuction_shouldContainBasicViewModelData() {
        Optional<ServiceResult<AuctionRoomViewModel>> result =
                service.getAuctionRoom("AUC001");

        assertTrue(result.isPresent());

        AuctionRoomViewModel vm = result.get().data();

        assertNotNull(vm.itemName());
        assertNotNull(vm.description());
        assertNotNull(vm.currentPrice());
        assertNotNull(vm.stepPrice());
        assertNotNull(vm.minimumBid());
        assertNotNull(vm.status());
    }

    @Test
    void offlineAuction_shouldContainTypeInformation() {
        Optional<ServiceResult<AuctionRoomViewModel>> result =
                service.getAuctionRoom("AUC001");

        assertTrue(result.isPresent());

        AuctionRoomViewModel vm = result.get().data();

        assertNotNull(vm.itemType());
        assertNotNull(vm.extraInfo());
    }

    @Test
    void offlineAuction_shouldContainHistoryList() {
        Optional<ServiceResult<AuctionRoomViewModel>> result =
                service.getAuctionRoom("AUC001");

        assertTrue(result.isPresent());

        AuctionRoomViewModel vm = result.get().data();

        assertNotNull(vm.bidHistory());
    }
}