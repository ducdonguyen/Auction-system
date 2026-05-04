package com.auction.server.core;

import com.auction.server.dao.UserDao;
import com.auction.server.util.PasswordUtil;
import com.auction.shared.models.AuthUser;
import com.auction.shared.network.LoginRequest;
import com.auction.shared.network.RegistrationRequest;
import com.auction.shared.network.ServiceResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.sql.SQLException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ServerAuthServiceTest {

    @Mock
    private UserDao userDao;

    private ServerAuthService authService;

    @BeforeEach
    void setUp() {
        authService = new ServerAuthService(userDao);
    }

    @Test
    void testLoginSuccess() throws SQLException {
        try (var mockedPasswordUtil = mockStatic(PasswordUtil.class)) {
            String username = "testuser";
            String password = "password123";
            String hash = "hashed_password";
            AuthUser user = new AuthUser("Full Name", username, "test@example.com", hash);

            when(userDao.findByUsername(username)).thenReturn(user);
            mockedPasswordUtil.when(() -> PasswordUtil.matches(password, hash)).thenReturn(true);

            LoginRequest request = new LoginRequest(username, password);
            ServiceResult<AuthUser> result = authService.processLogin(request);

            assertTrue(result.success());
            assertEquals(username, result.data().getUsername());
            verify(userDao).findByUsername(username);
        }
    }

    @Test
    void testLoginUserNotFound() throws SQLException {
        when(userDao.findByUsername("unknown")).thenReturn(null);

        LoginRequest request = new LoginRequest("unknown", "pass");
        ServiceResult<AuthUser> result = authService.processLogin(request);

        assertFalse(result.success());
        assertEquals("Tài khoản không tồn tại trên hệ thống.", result.message());
    }

    @Test
    void testLoginWrongPassword() throws SQLException {
        try (var mockedPasswordUtil = mockStatic(PasswordUtil.class)) {
            String username = "testuser";
            String password = "wrong_password";
            String hash = "hashed_correct_password";
            AuthUser user = new AuthUser("Full Name", username, "test@example.com", hash);

            when(userDao.findByUsername(username)).thenReturn(user);
            mockedPasswordUtil.when(() -> PasswordUtil.matches(password, hash)).thenReturn(false);

            LoginRequest request = new LoginRequest(username, password);
            ServiceResult<AuthUser> result = authService.processLogin(request);

            assertFalse(result.success());
            assertEquals("Mật khẩu không chính xác. Vui lòng thử lại.", result.message());
        }
    }

    @Test
    void testRegisterSuccess() throws SQLException {
        try (var mockedPasswordUtil = mockStatic(PasswordUtil.class)) {
            RegistrationRequest request = new RegistrationRequest("New User", "newuser", "new@example.com", "pass123");
            when(userDao.existsByUsernameOrEmail(request.username(), request.email())).thenReturn(false);
            mockedPasswordUtil.when(() -> PasswordUtil.hashPassword(anyString())).thenReturn("hashed_pass");

            ServiceResult<AuthUser> result = authService.processRegister(request);

            assertTrue(result.success());
            assertNotNull(result.data());
            verify(userDao).register(any(AuthUser.class));
        }
    }

    @Test
    void testRegisterDuplicate() throws SQLException {
        RegistrationRequest request = new RegistrationRequest("New User", "existing", "existing@example.com", "pass123");
        when(userDao.existsByUsernameOrEmail(request.username(), request.email())).thenReturn(true);

        ServiceResult<AuthUser> result = authService.processRegister(request);

        assertFalse(result.success());
        assertEquals("Tên đăng nhập hoặc email đã tồn tại.", result.message());
        verify(userDao, never()).register(any());
    }

    @Test
    void testDatabaseError() throws SQLException {
        when(userDao.findByUsername(any())).thenThrow(new SQLException("DB Down"));

        LoginRequest request = new LoginRequest("user", "pass");
        ServiceResult<AuthUser> result = authService.processLogin(request);

        assertFalse(result.success());
        assertTrue(result.message().contains("Lỗi kết nối"));
    }
}
