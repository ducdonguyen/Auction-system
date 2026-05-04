package com.auction.server.repository;

import com.auction.server.config.DatabaseConfig;
import com.auction.shared.models.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuctionRepositoryTest {

    @Mock
    private Connection connection;
    @Mock
    private PreparedStatement preparedStatement;
    @Mock
    private ResultSet resultSet;

    private AuctionRepository repository;

    @BeforeEach
    void setUp() {
        repository = new AuctionRepository();
    }

    @Test
    @DisplayName("Kiểm thử findById thành công (Art)")
    void testFindByIdArt() throws SQLException {
        try (var mockedDatabase = mockStatic(DatabaseConfig.class)) {
            mockedDatabase.when(DatabaseConfig::getConnection).thenReturn(connection);
            when(connection.prepareStatement(anyString())).thenReturn(preparedStatement);
            when(preparedStatement.executeQuery()).thenReturn(resultSet);
            
            // Giả lập ResultSet cho Art
            when(resultSet.next()).thenReturn(true, false);
            when(resultSet.getString("id")).thenReturn("AUC-1");
            when(resultSet.getString("item_name")).thenReturn("Painting");
            when(resultSet.getString("item_description")).thenReturn("Desc");
            when(resultSet.getDouble("item_starting_price")).thenReturn(100.0);
            when(resultSet.getString("item_type")).thenReturn("ART");
            when(resultSet.getString("item_extra_info")).thenReturn("Artist");
            when(resultSet.getString("seller_username")).thenReturn("seller1");
            when(resultSet.getTimestamp("start_time")).thenReturn(Timestamp.valueOf(LocalDateTime.now()));
            when(resultSet.getTimestamp("end_time")).thenReturn(Timestamp.valueOf(LocalDateTime.now().plusHours(1)));
            when(resultSet.getDouble("current_price")).thenReturn(150.0);
            when(resultSet.getDouble("step_price")).thenReturn(10.0);
            when(resultSet.getString("status")).thenReturn("RUNNING");
            when(resultSet.getString("highest_bidder_username")).thenReturn("bidder1");

            Auction auction = repository.findById("AUC-1");

            assertNotNull(auction);
            assertEquals("AUC-1", auction.getAuctionId());
            assertTrue(auction.getItem() instanceof Art);
            assertEquals("RUNNING", auction.getStatus().name());
            assertEquals("bidder1", auction.getHighestBidder().getUsername());
        }
    }

    @Test
    @DisplayName("Kiểm thử save auction")
    void testSave() throws SQLException {
        try (var mockedDatabase = mockStatic(DatabaseConfig.class)) {
            mockedDatabase.when(DatabaseConfig::getConnection).thenReturn(connection);
            when(connection.prepareStatement(anyString())).thenReturn(preparedStatement);

            Item item = new Electronics("Phone", "Desc", 500.0, 12);
            Seller seller = new Seller("seller1", "pass");
            Auction auction = new Auction("AUC-2", item, seller, 500.0, 50.0, LocalDateTime.now(), LocalDateTime.now().plusHours(2));

            repository.save(auction);

            verify(preparedStatement).executeUpdate();
            verify(preparedStatement).setString(1, "AUC-2");
            verify(preparedStatement).setString(6, "ELECTRONICS");
            verify(preparedStatement).setString(7, "12");
        }
    }

    @Test
    @DisplayName("Kiểm thử findByStatusAndStartTimeBefore")
    void testFindByStatusAndTime() throws SQLException {
        try (var mockedDatabase = mockStatic(DatabaseConfig.class)) {
            mockedDatabase.when(DatabaseConfig::getConnection).thenReturn(connection);
            when(connection.prepareStatement(anyString())).thenReturn(preparedStatement);
            when(preparedStatement.executeQuery()).thenReturn(resultSet);
            when(resultSet.next()).thenReturn(true, false);
            
            // Mock mapResultSetToAuction needs many calls
            when(resultSet.getString("id")).thenReturn("AUC-3");
            when(resultSet.getString("item_name")).thenReturn("V");
            when(resultSet.getString("item_type")).thenReturn("VEHICLE");
            when(resultSet.getString("item_extra_info")).thenReturn("Toyota");
            when(resultSet.getTimestamp("start_time")).thenReturn(Timestamp.valueOf(LocalDateTime.now()));
            when(resultSet.getTimestamp("end_time")).thenReturn(Timestamp.valueOf(LocalDateTime.now()));
            when(resultSet.getString("status")).thenReturn("OPEN");

            List<Auction> list = repository.findByStatusAndStartTimeBefore(AuctionStatus.OPEN, LocalDateTime.now());

            assertFalse(list.isEmpty());
            assertEquals(1, list.size());
            assertEquals("AUC-3", list.get(0).getAuctionId());
        }
    }
}
