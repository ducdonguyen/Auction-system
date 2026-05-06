package com.auction.server.core;

import com.auction.server.dao.UserDao;
import com.auction.shared.models.AuthUser;
import com.auction.shared.network.LoginRequest;
import com.auction.shared.network.RegistrationRequest;
import com.auction.shared.network.ServiceResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.sql.SQLException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class ServerAuthServiceTest {

    private ServerAuthService authService;
    private UserDao userDao;

    @BeforeEach
    void setUp() {
        userDao = mock(UserDao.class);
        authService = new ServerAuthService(userDao);
    }

    @Test
    @DisplayName("Kiểm thử đăng nhập thành công")
    void testLoginSuccess() throws SQLException {
        String pass = "pass123";
        String hash = "$2a$10$8K1p/a06v.2v5L/vL2/v2.m6.S7.C7.U7.X7.R7.O7.T7.H7.S7.I7"; // Dummy hash
        // Note: PasswordUtil.hashPassword is slow, but we can mock it or use a real one since it's a small util.
        // Let's use real PasswordUtil since it's already tested.
        String realHash = com.auction.server.util.PasswordUtil.hashPassword(pass);
        
        AuthUser user = new AuthUser("Full Name", "user1", "email@test.com", realHash);
        when(userDao.findByUsername("user1")).thenReturn(user);

        ServiceResult<AuthUser> result = authService.processLogin(new LoginRequest("user1", pass));

        assertTrue(result.success());
        assertEquals(user, result.data());
        assertTrue(result.message().contains("thành công"));
    }

    @Test
    @DisplayName("Kiểm thử đăng nhập thất bại - Sai mật khẩu")
    void testLoginWrongPassword() throws SQLException {
        String realHash = com.auction.server.util.PasswordUtil.hashPassword("correct");
        AuthUser user = new AuthUser("Full Name", "user1", "email@test.com", realHash);
        when(userDao.findByUsername("user1")).thenReturn(user);

        ServiceResult<AuthUser> result = authService.processLogin(new LoginRequest("user1", "wrong"));

        assertFalse(result.success());
        assertNull(result.data());
        assertTrue(result.message().contains("không chính xác"));
    }

    @Test
    @DisplayName("Kiểm thử đăng nhập thất bại - Không tìm thấy user")
    void testLoginUserNotFound() throws SQLException {
        when(userDao.findByUsername("missing")).thenReturn(null);

        ServiceResult<AuthUser> result = authService.processLogin(new LoginRequest("missing", "any"));

        assertFalse(result.success());
        assertTrue(result.message().contains("không tồn tại"));
    }

    @Test
    @DisplayName("Kiểm thử đăng ký thành công")
    void testRegisterSuccess() throws SQLException {
        when(userDao.existsByUsernameOrEmail(anyString(), anyString())).thenReturn(false);

        RegistrationRequest req = new RegistrationRequest("Name", "newuser", "new@test.com", "pass");
        ServiceResult<AuthUser> result = authService.processRegister(req);

        assertTrue(result.success());
        assertNotNull(result.data());
        assertEquals("newuser", result.data().getUsername());
        verify(userDao).register(any(AuthUser.class));
    }

    @Test
    @DisplayName("Kiểm thử đăng ký thất bại - Trùng username/email")
    void testRegisterDuplicate() throws SQLException {
        when(userDao.existsByUsernameOrEmail("user1", "email@test.com")).thenReturn(true);

        RegistrationRequest req = new RegistrationRequest("Name", "user1", "email@test.com", "pass");
        ServiceResult<AuthUser> result = authService.processRegister(req);

        assertFalse(result.success());
        assertTrue(result.message().contains("đã tồn tại"));
        verify(userDao, never()).register(any());
    }

    @Test
    @DisplayName("Kiểm thử lỗi SQL khi đăng nhập")
    void testLoginSQLException() throws SQLException {
        when(userDao.findByUsername(anyString())).thenThrow(new SQLException("DB Error"));

        ServiceResult<AuthUser> result = authService.processLogin(new LoginRequest("u", "p"));

        assertFalse(result.success());
        assertTrue(result.message().contains("MySQL"));
    }
}
