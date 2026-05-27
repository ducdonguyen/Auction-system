package com.auction.server.repository;

import com.auction.server.config.DatabaseConfig;
import com.auction.shared.models.Auction;
import com.auction.shared.models.AuctionStatus;
import com.auction.shared.models.Item;
import com.auction.shared.models.ItemFactory;
import com.auction.shared.models.Seller;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.Mockito.*;

public class AuctionRepositoryTest {

    private AuctionRepository repository;
    private MockedStatic<DatabaseConfig> mockedDatabaseConfig;
    private Connection mockConnection;
    private PreparedStatement mockPreparedStatement;
    private ResultSet mockResultSet;

    @BeforeEach
    public void setUp() throws Exception {
        repository = new AuctionRepository();
        mockConnection = mock(Connection.class);
        mockPreparedStatement = mock(PreparedStatement.class);
        mockResultSet = mock(ResultSet.class);

        mockedDatabaseConfig = mockStatic(DatabaseConfig.class);
        mockedDatabaseConfig.when(DatabaseConfig::getConnection).thenReturn(mockConnection);

        when(mockConnection.prepareStatement(anyString())).thenReturn(mockPreparedStatement);
    }

    @AfterEach
    public void tearDown() {
        if (mockedDatabaseConfig != null && !mockedDatabaseConfig.isClosed()) {
            mockedDatabaseConfig.close();
        }
    }

    private void setupMockAuctionData(ResultSet rs) throws SQLException {
        when(rs.getString("id")).thenReturn("AUC-123");
        when(rs.getString("item_name")).thenReturn("Laptop Dell");
        when(rs.getString("item_description")).thenReturn("Máy còn mới");
        when(rs.getString("item_type")).thenReturn("ELECTRONICS");
        when(rs.getString("item_extra_info")).thenReturn("12");
        when(rs.getDouble("item_starting_price")).thenReturn(100000.0);
        when(rs.getString("seller_username")).thenReturn("trongduy");
        when(rs.getString("seller_full_name")).thenReturn("Trọng Duy");
        when(rs.getDouble("current_price")).thenReturn(150000.0);
        when(rs.getDouble("step_price")).thenReturn(10000.0);
        when(rs.getTimestamp("start_time")).thenReturn(Timestamp.valueOf(LocalDateTime.now()));
        when(rs.getTimestamp("end_time")).thenReturn(Timestamp.valueOf(LocalDateTime.now().plusDays(1)));
        when(rs.getString("status")).thenReturn("RUNNING");
        when(rs.getString("highest_bidder_username")).thenReturn("nguoimua");
        when(rs.getString("bidder_full_name")).thenReturn("Khách Hàng VIP");
    }

    @Test
    public void testInit_Success() throws Exception {
        repository.init();
        mockedDatabaseConfig.verify(DatabaseConfig::initializeDatabase, times(1));
    }

    // NHÁNH MỚI: Bắt nhánh lỗi SQLException trong hàm init()
    @Test
    public void testInit_SQLException() throws Exception {
        mockedDatabaseConfig.when(DatabaseConfig::initializeDatabase).thenThrow(new SQLException("Lỗi DB"));
        repository.init();
    }

    @Test
    public void testFindAll_Success() throws Exception {
        when(mockPreparedStatement.executeQuery()).thenReturn(mockResultSet);
        when(mockResultSet.next()).thenReturn(true).thenReturn(false);
        setupMockAuctionData(mockResultSet);

        List<Auction> results = repository.findAll();
        assertEquals(1, results.size());
    }

    // NHÁNH MỚI: Quét qua các trường hợp Tên thật bị Null hoặc chuỗi rỗng (" ")
    @Test
    public void testFindAll_WithNullAndEmptyNames() throws Exception {
        when(mockPreparedStatement.executeQuery()).thenReturn(mockResultSet);
        when(mockResultSet.next()).thenReturn(true).thenReturn(false);
        setupMockAuctionData(mockResultSet);

        when(mockResultSet.getString("seller_full_name")).thenReturn("   "); // Chuỗi khoảng trắng
        when(mockResultSet.getString("highest_bidder_username")).thenReturn("nguoimua");
        when(mockResultSet.getString("bidder_full_name")).thenReturn(null); // Bị null

        List<Auction> results = repository.findAll();
        assertEquals(1, results.size());
    }

    @Test
    public void testFindAll_SQLException() throws Exception {
        when(mockPreparedStatement.executeQuery()).thenThrow(new SQLException("Mất kết nối Database"));
        List<Auction> results = repository.findAll();
        assertTrue(results.isEmpty());
    }

    @Test
    public void testFindPendingAuctions() throws Exception {
        when(mockPreparedStatement.executeQuery()).thenReturn(mockResultSet);
        when(mockResultSet.next()).thenReturn(true).thenReturn(false);
        setupMockAuctionData(mockResultSet);
        when(mockResultSet.getString("status")).thenReturn("PENDING");

        List<Auction> pending = repository.findPendingAuctions();
        assertEquals(1, pending.size());
    }

    @Test
    public void testFindById_WithBidHistory() throws Exception {
        PreparedStatement mockPsAuction = mock(PreparedStatement.class);
        PreparedStatement mockPsBids = mock(PreparedStatement.class);
        ResultSet mockRsAuction = mock(ResultSet.class);
        ResultSet mockRsBids = mock(ResultSet.class);

        when(mockConnection.prepareStatement(contains("auctions"))).thenReturn(mockPsAuction);
        when(mockConnection.prepareStatement(contains("bid_transactions"))).thenReturn(mockPsBids);

        when(mockPsAuction.executeQuery()).thenReturn(mockRsAuction);
        when(mockRsAuction.next()).thenReturn(true).thenReturn(false);
        setupMockAuctionData(mockRsAuction);

        when(mockPsBids.executeQuery()).thenReturn(mockRsBids);
        when(mockRsBids.next()).thenReturn(true).thenReturn(false);
        when(mockRsBids.getString("id")).thenReturn("TX-001");
        when(mockRsBids.getString("bidder_username")).thenReturn("nguoimua");
        when(mockRsBids.getString("bidder_full_name")).thenReturn(null); // Test nhánh null trong lịch sử
        when(mockRsBids.getDouble("bid_amount")).thenReturn(120000.0);
        when(mockRsBids.getTimestamp("bid_time")).thenReturn(Timestamp.valueOf(LocalDateTime.now()));

        Auction auction = repository.findById("AUC-123");
        assertNotNull(auction);
    }

    // NHÁNH MỚI: Truy vấn phòng không tồn tại (auction = null)
    @Test
    public void testFindById_NotFound() throws Exception {
        PreparedStatement mockPsAuction = mock(PreparedStatement.class);
        when(mockConnection.prepareStatement(contains("auctions"))).thenReturn(mockPsAuction);
        when(mockPsAuction.executeQuery()).thenReturn(mockResultSet);
        when(mockResultSet.next()).thenReturn(false);

        Auction auction = repository.findById("AUC-404");
        assertNull(auction);
    }

    @Test
    public void testFindByStatusAndStartTimeBefore() throws Exception {
        when(mockPreparedStatement.executeQuery()).thenReturn(mockResultSet);
        when(mockResultSet.next()).thenReturn(true).thenReturn(false);
        setupMockAuctionData(mockResultSet);

        List<Auction> results = repository.findByStatusAndStartTimeBefore(AuctionStatus.OPEN, LocalDateTime.now());
        assertEquals(1, results.size());
    }

    // NHÁNH MỚI: Lỗi truy vấn trong hàm Helper findBy
    @Test
    public void testFindByStatusAndEndTimeBefore_SQLException() throws Exception {
        when(mockPreparedStatement.executeQuery()).thenThrow(new SQLException("Lỗi truy vấn helper"));
        List<Auction> results = repository.findByStatusAndEndTimeBefore(AuctionStatus.RUNNING, LocalDateTime.now());
        assertTrue(results.isEmpty());
    }

    @Test
    public void testSave() throws Exception {
        Item item = ItemFactory.createItem("ELECTRONICS", "iPhone", "Mới", 20000000, "12");
        Auction auction = new Auction("AUC-999", item, new Seller("admin", ""), 20000000, 500000, LocalDateTime.now(), LocalDateTime.now().plusDays(3));
        auction.setStatus(AuctionStatus.OPEN);

        when(mockPreparedStatement.executeUpdate()).thenReturn(1);
        Auction saved = repository.save(auction);
        assertNotNull(saved);
        verify(mockPreparedStatement, times(1)).executeUpdate();
    }

    // NHÁNH MỚI: Lưu dữ liệu với các Object rỗng để test toàn bộ toán tử ba ngôi (item != null ? ...)
    @Test
    public void testSave_WithNullFields() throws Exception {
        Auction auction = new Auction("AUC-NULLS", null, null, 0, 0, LocalDateTime.now(), LocalDateTime.now());
        auction.setStatus(AuctionStatus.PENDING);

        when(mockPreparedStatement.executeUpdate()).thenReturn(1);
        Auction saved = repository.save(auction);
        assertNotNull(saved);
        verify(mockPreparedStatement, times(1)).executeUpdate();
    }

    // NHÁNH MỚI: Bắt lỗi khi save() quăng Exception
    @Test
    public void testSave_SQLException() throws Exception {
        Auction auction = new Auction("AUC-ERR", null, null, 0, 0, LocalDateTime.now(), LocalDateTime.now());
        auction.setStatus(AuctionStatus.OPEN);
        when(mockPreparedStatement.executeUpdate()).thenThrow(new SQLException("Lỗi Save DB"));

        repository.save(auction); // Xác nhận app không crash
    }
}