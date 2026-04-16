package com.auction.client.service;

import com.auction.client.dao.UserDao;
import com.auction.client.model.LoginRequest;
import com.auction.client.model.RegistrationRequest;
import com.auction.client.model.ServiceResult;
import com.auction.client.util.PasswordUtil;
import com.auction.shared.models.AuthUser;

import java.sql.SQLException;

public class AuthService {

    private final UserDao userDao = new UserDao();

    public void initializeDatabase() throws SQLException {
        userDao.initializeDatabase();
        // Tự động tạo tài khoản mẫu nếu chưa có
        if (userDao.findByUsername("admin") == null) {
            AuthUser admin = new AuthUser("Quản trị viên", "admin", "admin@auction.com", PasswordUtil.hashPassword("123456"));
            userDao.register(admin);
        }
    }

    public ServiceResult<AuthUser> login(LoginRequest request) {
        String username = (request.username() == null) ? "" : request.username().trim();
        String password = (request.password() == null) ? "" : request.password().trim();

        if (username.isEmpty() || password.isEmpty()) {
            return new ServiceResult<>(false, "Vui lòng nhập đầy đủ tên đăng nhập và mật khẩu.", null);
        }

        try {
            AuthUser user = userDao.findByUsername(username);
            if (user == null) {
                return new ServiceResult<>(false, "Tài khoản không tồn tại trên hệ thống.", null);
            }
            if (!PasswordUtil.matches(password, user.getPasswordHash())) {
                return new ServiceResult<>(false, "Mật khẩu không chính xác. Vui lòng thử lại.", null);
            }
            
            // Lưu thông tin vào phiên làm việc
            SessionContext.setCurrentUser(user);
            return new ServiceResult<>(true, "Đăng nhập thành công! Đang chuyển hướng...", user);
        } catch (SQLException exception) {
            return new ServiceResult<>(false, "Lỗi kết nối cơ sở dữ liệu (MySQL).", null);
        }
    }

    public ServiceResult<AuthUser> register(RegistrationRequest request) {
        try {
            if (userDao.existsByUsernameOrEmail(request.username(), request.email())) {
                return new ServiceResult<>(false, "Tên đăng nhập hoặc email đã tồn tại.", null);
            }

            AuthUser user = new AuthUser(
                request.fullName(), 
                request.username(), 
                request.email(), 
                PasswordUtil.hashPassword(request.password())
            );
            userDao.register(user);
            return new ServiceResult<>(true, "Đăng ký thành công! Hãy đăng nhập ngay.", user);
        } catch (SQLException exception) {
            return new ServiceResult<>(false, "Lỗi: Không thể lưu tài khoản.", null);
        }
    }
}
