package com.auction.server.service;

import com.auction.server.dao.UserDao;
import com.auction.server.util.PasswordUtil;
import com.auction.shared.models.auth.UserAccount;
import com.auction.shared.network.requests.LoginRequest;
import com.auction.shared.network.requests.RegistrationRequest;
import com.auction.shared.network.responses.ServiceResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.sql.SQLException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class AuthServicesTest {

    private UserDao mockUserDao;
    private AuthService authService;

    @BeforeEach
    void setUp() {
        // 1. Tạo bản mock bằng lệnh trực tiếp của Mockito
        mockUserDao = mock(UserDao.class);

        // 2. Nạp trực tiếp bản mock vào qua Constructor cửa sau ta vừa tạo
        authService = new AuthService(mockUserDao);
    }

    @Test
    @DisplayName("Kiểm thử Đăng nhập thành công - Server")
    void testLogin_Success() throws Exception {
        String plainPassword = "password123";
        String hashedPassword = PasswordUtil.hashPassword(plainPassword);
        UserAccount dbUser = new UserAccount(1L, "Nguyen Van A", "bimbim", "an@email.com", hashedPassword, "USER", 10000000.0);

        // Đảm bảo mock hoạt động
        when(mockUserDao.findByUsername("bimbim")).thenReturn(dbUser);

        LoginRequest request = new LoginRequest("bimbim", plainPassword);
        ServiceResult<UserAccount> result = authService.login(request);

        assertTrue(result.success());
        assertEquals("Đăng nhập thành công!", result.message());
        assertNotNull(result.data());
        assertNull(result.data().getPasswordHash());
    }

    @Test
    @DisplayName("Kiểm thử Đăng nhập thất bại - Thiếu thông tin nhập vào")
    void testLogin_MissingInfo() {
        LoginRequest request = new LoginRequest("", "   ");
        ServiceResult<UserAccount> result = authService.login(request);

        assertFalse(result.success());
        assertEquals("Vui lòng nhập đủ thông tin.", result.message());
    }

    @Test
    @DisplayName("Kiểm thử Đăng nhập thất bại - Tài khoản không tồn tại")
    void testLogin_UserNotFound() throws Exception {
        when(mockUserDao.findByUsername("unknown_user")).thenReturn(null);

        LoginRequest request = new LoginRequest("unknown_user", "anyPass");
        ServiceResult<UserAccount> result = authService.login(request);

        assertFalse(result.success());
        assertEquals("Tài khoản không tồn tại.", result.message());
    }

    @Test
    @DisplayName("Kiểm thử Đăng nhập thất bại - Sai mật khẩu")
    void testLogin_WrongPassword() throws Exception {
        String hashedPassword = PasswordUtil.hashPassword("correct_pass");
        UserAccount dbUser = new UserAccount(1L, "Nguyen Van A", "bimbim", "an@email.com", hashedPassword, "USER", 10000000.0);

        when(mockUserDao.findByUsername("bimbim")).thenReturn(dbUser);

        LoginRequest request = new LoginRequest("bimbim", "wrong_pass");
        ServiceResult<UserAccount> result = authService.login(request);

        assertFalse(result.success());
        assertEquals("Mật khẩu không chính xác.", result.message());
    }

    @Test
    @DisplayName("Kiểm thử Đăng nhập thất bại - Lỗi kết nối Cơ sở dữ liệu")
    void testLogin_DatabaseError() throws Exception {
        when(mockUserDao.findByUsername(anyString())).thenThrow(new SQLException("Mất kết nối MySQL"));

        LoginRequest request = new LoginRequest("user", "pass");
        ServiceResult<UserAccount> result = authService.login(request);

        assertFalse(result.success());
        assertEquals("Lỗi kết nối cơ sở dữ liệu trên Server.", result.message());
    }

    @Test
    @DisplayName("Kiểm thử Đăng ký thành công - Server")
    void testRegister_Success() throws Exception {
        RegistrationRequest request = new RegistrationRequest("User Moi", "newuser", "new@email.com", "mypass");

        when(mockUserDao.existsByUsernameOrEmail("newuser", "new@email.com")).thenReturn(false);

        ServiceResult<UserAccount> result = authService.register(request);

        assertTrue(result.success());
        assertEquals("Đăng ký tài khoản thành công!", result.message());
        assertNotNull(result.data());
        assertNull(result.data().getPasswordHash());

        verify(mockUserDao, times(1)).register(any(UserAccount.class));
    }

    @Test
    @DisplayName("Kiểm thử Đăng ký thất bại - Trùng tên đăng nhập hoặc Email")
    void testRegister_AlreadyExists() throws Exception {
        RegistrationRequest request = new RegistrationRequest("Trùng Tên", "duplicate", "dup@email.com", "pass");

        when(mockUserDao.existsByUsernameOrEmail("duplicate", "dup@email.com")).thenReturn(true);

        ServiceResult<UserAccount> result = authService.register(request);

        assertFalse(result.success());
        assertEquals("Tên đăng nhập hoặc Email đã tồn tại.", result.message());
        verify(mockUserDao, never()).register(any(UserAccount.class));
    }
}