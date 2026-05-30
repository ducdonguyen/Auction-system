package com.auction.server.dao;

import com.auction.server.config.DatabaseConfig;
import com.auction.shared.models.auth.UserAccount;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

class UserDaoTest {

    private UserDao userDao;
    private Connection mockConnection;
    private PreparedStatement mockPreparedStatement;
    private ResultSet mockResultSet;
    private MockedStatic<DatabaseConfig> mockedDatabaseConfig;

    @BeforeEach
    void setUp() throws SQLException {
        userDao = new UserDao();
        mockConnection = mock(Connection.class);
        mockPreparedStatement = mock(PreparedStatement.class);
        mockResultSet = mock(ResultSet.class);
        mockedDatabaseConfig = mockStatic(DatabaseConfig.class);
        
        when(DatabaseConfig.getConnection()).thenReturn(mockConnection);
        when(mockConnection.prepareStatement(anyString())).thenReturn(mockPreparedStatement);
    }

    @AfterEach
    void tearDown() {
        mockedDatabaseConfig.close();
    }

    @Test
    @DisplayName("Nên trả về true nếu user tồn tại")
    void testExistsByUsernameOrEmail_Exists() throws SQLException {
        when(mockPreparedStatement.executeQuery()).thenReturn(mockResultSet);
        when(mockResultSet.next()).thenReturn(true);
        when(mockResultSet.getInt(1)).thenReturn(1);

        boolean exists = userDao.existsByUsernameOrEmail("user", "email");
        assertTrue(exists);
        verify(mockPreparedStatement).setString(1, "user");
        verify(mockPreparedStatement).setString(2, "email");
    }

    @Test
    @DisplayName("Nên trả về false nếu user không tồn tại")
    void testExistsByUsernameOrEmail_NotExists() throws SQLException {
        when(mockPreparedStatement.executeQuery()).thenReturn(mockResultSet);
        when(mockResultSet.next()).thenReturn(true);
        when(mockResultSet.getInt(1)).thenReturn(0);

        boolean exists = userDao.existsByUsernameOrEmail("user", "email");
        assertFalse(exists);
    }

    @Test
    @DisplayName("Nên gọi executeUpdate khi đăng ký")
    void testRegister() throws SQLException {
        UserAccount user = new UserAccount("Full Name", "user", "email", "hash", "USER");
        userDao.register(user);

        verify(mockPreparedStatement).setString(1, "Full Name");
        verify(mockPreparedStatement).setString(2, "user");
        verify(mockPreparedStatement).setString(3, "email");
        verify(mockPreparedStatement).setString(4, "hash");
        verify(mockPreparedStatement).executeUpdate();
    }

    @Test
    @DisplayName("Nên tìm thấy user theo username")
    void testFindByUsername_Found() throws SQLException {
        when(mockPreparedStatement.executeQuery()).thenReturn(mockResultSet);
        when(mockResultSet.next()).thenReturn(true);
        when(mockResultSet.getString("full_name")).thenReturn("Full Name");
        when(mockResultSet.getString("username")).thenReturn("user");
        when(mockResultSet.getString("email")).thenReturn("email");
        when(mockResultSet.getString("password_hash")).thenReturn("hash");
        when(mockResultSet.getLong("id")).thenReturn(1L);

        UserAccount user = userDao.findByUsername("user");
        assertNotNull(user);
        assertEquals("user", user.getUsername());
        assertEquals(1L, user.getId());
    }

    @Test
    @DisplayName("Nên trả về null nếu không tìm thấy user")
    void testFindByUsername_NotFound() throws SQLException {
        when(mockPreparedStatement.executeQuery()).thenReturn(mockResultSet);
        when(mockResultSet.next()).thenReturn(false);

        UserAccount user = userDao.findByUsername("user");
        assertNull(user);
    }
    @Test
    @DisplayName("Nên ném ngoại lệ SQLException trong existsByUsernameOrEmail")
    void testExistsByUsernameOrEmail_SQLException() throws SQLException {
        when(mockPreparedStatement.executeQuery()).thenThrow(new SQLException("DB Error"));
        assertThrows(SQLException.class, () -> userDao.existsByUsernameOrEmail("user", "email"));
    }

    @Test
    @DisplayName("Nên ném ngoại lệ SQLException trong register")
    void testRegister_SQLException() throws SQLException {
        when(mockPreparedStatement.executeUpdate()).thenThrow(new SQLException("DB Error"));
        UserAccount user = new UserAccount("Full Name", "user", "email", "hash", "USER");
        assertThrows(SQLException.class, () -> userDao.register(user));
    }

    @Test
    @DisplayName("Nên ném ngoại lệ SQLException trong findByUsername")
    void testFindByUsername_SQLException() throws SQLException {
        when(mockPreparedStatement.executeQuery()).thenThrow(new SQLException("DB Error"));
        assertThrows(SQLException.class, () -> userDao.findByUsername("user"));
    }
}
