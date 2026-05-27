package com.auction.client.service;

import com.auction.client.service.AuctionCatalogService;
import com.auction.client.service.AuctionRoomService;

/**
 * Lớp quản lý khởi tạo và cung cấp các Service (Service Locator Pattern).
 * Hỗ trợ Dependency Inversion để dễ dàng thay thế bằng Mock Object trong Unit Test.
 */
public class ServiceFactory {

    // Khởi tạo các instance thật mặc định dùng cho môi trường Production
    private static AuctionCatalogService catalogService = new AuctionCatalogService();
    private static AuctionRoomService roomService = new AuctionRoomService();
    private static AuthService authService = new AuthService();

    /**
     * Lấy instance của AuctionCatalogService.
     * Sẽ trả về Mock Object nếu đã được set trong môi trường Test.
     */
    public static AuctionCatalogService getCatalogService() {
        return catalogService;
    }

    /**
     * Lấy instance của AuctionRoomService.
     */
    public static AuctionRoomService getRoomService() {
        return roomService;
    }

    /**
     * Lấy instance của AuthService.
     */
    public static AuthService getAuthService() {
        return authService;
    }

    // --------------------------------------------------------
    // CÁC HÀM HỖ TRỢ DÀNH RIÊNG CHO MÔI TRƯỜNG UNIT TEST
    // --------------------------------------------------------

    public static void setMockCatalogService(AuctionCatalogService mockService) {
        catalogService = mockService;
    }

    public static void setMockRoomService(AuctionRoomService mockService) {
        roomService = mockService;
    }

    /**
     * Reset lại các service về nguyên bản (Nên gọi trong hàm @AfterEach của Unit Test)
     */
    public static void resetToDefault() {
        catalogService = new AuctionCatalogService();
        roomService = new AuctionRoomService();
    }
}
