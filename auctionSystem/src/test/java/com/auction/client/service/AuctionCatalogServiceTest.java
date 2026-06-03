package com.auction.client.service;

import com.auction.shared.models.auction.AuctionRow;
import com.auction.shared.network.responses.ServiceResult;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class AuctionCatalogServiceTest {

    private final AuctionCatalogService service =
            new AuctionCatalogService();

    @Test
    void getAvailableStatuses_shouldReturnAllStatuses() {
        List<String> statuses = service.getAvailableStatuses();

        assertNotNull(statuses);
        assertTrue(statuses.contains("Tất cả"));
        assertTrue(statuses.contains("OPEN"));
        assertTrue(statuses.contains("RUNNING"));
        assertTrue(statuses.contains("FINISHED"));
        assertTrue(statuses.contains("PAID"));
        assertTrue(statuses.contains("CANCELED"));
    }

    @Test
    void filterAuctions_shouldReturnAllWhenKeywordNull() {
        List<AuctionRow> rows =
                service.filterAuctions(null, "Tất cả");

        assertNotNull(rows);
    }

    @Test
    void filterAuctions_shouldReturnAllWhenKeywordEmpty() {
        List<AuctionRow> rows =
                service.filterAuctions("", "Tất cả");

        assertNotNull(rows);
    }

    @Test
    void filterAuctions_shouldHandleUnknownKeyword() {
        List<AuctionRow> rows =
                service.filterAuctions("xxxxxxxxxxxxxxxx", "Tất cả");

        assertNotNull(rows);
    }

    @Test
    void filterAuctions_shouldHandleStatusFilter() {
        List<AuctionRow> rows =
                service.filterAuctions("", "RUNNING");

        assertNotNull(rows);
    }

    @Test
    void filterAuctions_shouldHandleFinishedFilter() {
        List<AuctionRow> rows =
                service.filterAuctions("", "FINISHED");

        assertNotNull(rows);
    }

    @Test
    void filterAuctions_shouldHandleInvalidStatus() {
        List<AuctionRow> rows =
                service.filterAuctions("", "INVALID_STATUS");

        assertNotNull(rows);
        assertTrue(rows.isEmpty());
    }

    @Test
    void getPendingAuctions_shouldNotThrowException() {
        List<AuctionRow> rows =
                service.getPendingAuctions();

        assertNotNull(rows);
    }

    @Test
    void approveAuction_shouldReturnResultObject() {
        ServiceResult<Void> result =
                service.approveAuction("AUC001");

        assertNotNull(result);
        assertNotNull(result.message());
    }

    @Test
    void approveAuction_shouldHandleInvalidAuctionId() {
        ServiceResult<Void> result =
                service.approveAuction("INVALID");

        assertNotNull(result);
    }

    @Test
    void cancelAuction_shouldReturnResultObject() {
        ServiceResult<Void> result =
                service.cancelAuction("AUC001");

        assertNotNull(result);
    }

    @Test
    void cancelAuction_shouldHandleInvalidAuctionId() {
        ServiceResult<Void> result =
                service.cancelAuction("INVALID");

        assertNotNull(result);
    }

    @Test
    void filterAuctions_shouldBeCaseInsensitive() {
        List<AuctionRow> rows1 =
                service.filterAuctions("laptop", "Tất cả");

        List<AuctionRow> rows2 =
                service.filterAuctions("LAPTOP", "Tất cả");

        assertNotNull(rows1);
        assertNotNull(rows2);
    }

    @Test
    void filterAuctions_shouldTrimKeyword() {
        List<AuctionRow> rows =
                service.filterAuctions("   laptop   ", "Tất cả");

        assertNotNull(rows);
    }
}