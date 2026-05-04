package com.auction.server.dao;

import com.auction.server.config.DatabaseConfig;
import com.auction.shared.models.AuthUser;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserDaoTest {

    @Mock
    private Connection connection;
    @Mock
    private PreparedStatement preparedStatement;
    @Mock
    private ResultSet resultSet;

    private UserDao userDao;

    @BeforeEach
    void setUp() {
        userDao = new UserDao();
    }

    @Test
    @DisplayName("Kiểm thử findByUsername thành công")
    void testFindByUsernameSuccess() throws SQLException {
        try (var mockedDatabase = mockStatic(DatabaseConfig.class)) {
            mockedDatabase.when(DatabaseConfig::getConnection).thenReturn(connection);
            when(connection.prepareStatement(anyString())).thenReturn(preparedStatement);
            when(preparedStatement.executeQuery()).thenReturn(resultSet);
            when(resultSet.next()).thenReturn(true);
            when(resultSet.getLong("id")).thenReturn(1L);
            when(resultSet.getString("full_name")).thenReturn("Test User");
            when(resultSet.getString("username")).thenReturn("testuser");
            when(resultSet.getString("email")).thenReturn("test@test.com");
            when(resultSet.getString("password_hash")).thenReturn("hash");

            AuthUser user = userDao.findByUsername("testuser");

            assertNotNull(user);
            assertEquals("testuser", user.getUsername());
            assertEquals("test@test.com", user.getEmail());
            verify(preparedStatement).setString(1, "testuser");
        }
    }

    @Test
    @DisplayName("Kiểm thử findByUsername không tìm thấy")
    void testFindByUsernameNotFound() throws SQLException {
        try (var mockedDatabase = mockStatic(DatabaseConfig.class)) {
            mockedDatabase.when(DatabaseConfig::getConnection).thenReturn(connection);
            when(connection.prepareStatement(anyString())).thenReturn(preparedStatement);
            when(preparedStatement.executeQuery()).thenReturn(resultSet);
            when(resultSet.next()).thenReturn(false);

            AuthUser user = userDao.findByUsername("unknown");

            assertNull(user);
        }
    }

    @Test
    @DisplayName("Kiểm thử existsByUsernameOrEmail - trả về true")
    void testExistsTrue() throws SQLException {
        try (var mockedDatabase = mockStatic(DatabaseConfig.class)) {
            mockedDatabase.when(DatabaseConfig::getConnection).thenReturn(connection);
            when(connection.prepareStatement(anyString())).thenReturn(preparedStatement);
            when(preparedStatement.executeQuery()).thenReturn(resultSet);
            when(resultSet.next()).thenReturn(true);
            when(resultSet.getInt(1)).thenReturn(1);

            boolean exists = userDao.existsByUsernameOrEmail("user", "email");

            assertTrue(exists);
        }
    }

    @Test
    @DisplayName("Kiểm thử register - gọi executeUpdate")
    void testRegister() throws SQLException {
        try (var mockedDatabase = mockStatic(DatabaseConfig.class)) {
            mockedDatabase.when(DatabaseConfig::getConnection).thenReturn(connection);
            when(connection.prepareStatement(anyString())).thenReturn(preparedStatement);

            AuthUser user = new AuthUser("FN", "U", "E", "H");
            userDao.register(user);

            verify(preparedStatement).executeUpdate();
            verify(preparedStatement).setString(1, "FN");
            verify(preparedStatement).setString(2, "U");
            verify(preparedStatement).setString(3, "E");
            verify(preparedStatement).setString(4, "H");
        }
    }
}
