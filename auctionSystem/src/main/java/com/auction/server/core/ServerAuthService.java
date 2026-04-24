package com.auction.server.core;

import com.auction.server.dao.UserDao;
import com.auction.server.util.PasswordUtil; // Giả sử em đã dời PasswordUtil sang Server
import com.auction.shared.models.AuthUser;
import com.auction.shared.network.LoginRequest;
import com.auction.shared.network.RegistrationRequest;
import com.auction.shared.network.ServiceResult;

import java.sql.SQLException;

public class ServerAuthService {

    private final UserDao userDao = new UserDao();

    // ==========================================
    // 1. XỬ LÝ ĐĂNG NHẬP VÀ TRẢ VỀ THÔNG BÁO LỖI
    // ==========================================
    public ServiceResult<AuthUser> processLogin(LoginRequest request) {
        try {
            AuthUser user = userDao.findByUsername(request.username());

            // CÁC CÂU LỆNH EM MUỐN GIỮ LẠI NẰM Ở ĐÂY:
            if (user == null) {
                return new ServiceResult<>(false, "Tài khoản không tồn tại trên hệ thống.", null);
            }
            if (!PasswordUtil.matches(request.password(), user.getPasswordHash())) {
                return new ServiceResult<>(false, "Mật khẩu không chính xác. Vui lòng thử lại.", null);
            }

            return new ServiceResult<>(true, "Đăng nhập thành công! Đang chuyển hướng...", user);

        } catch (SQLException e) {
            return new ServiceResult<>(false, "Lỗi kết nối cơ sở dữ liệu (MySQL) từ phía Server.", null);
        }
    }

    // ==========================================
    // 2. XỬ LÝ ĐĂNG KÝ VÀ TRẢ VỀ THÔNG BÁO LỖI
    // ==========================================
    public ServiceResult<AuthUser> processRegister(RegistrationRequest request) {
        try {
            if (userDao.existsByUsernameOrEmail(request.username(), request.email())) {
                return new ServiceResult<>(false, "Tên đăng nhập hoặc email đã tồn tại.", null);
            }

            // Băm mật khẩu tại Server để bảo mật tối đa
            AuthUser user = new AuthUser(
                    request.fullName(),
                    request.username(),
                    request.email(),
                    PasswordUtil.hashPassword(request.password())
            );

            userDao.register(user);
            return new ServiceResult<>(true, "Đăng ký thành công! Hãy đăng nhập ngay.", user);

        } catch (SQLException e) {
            return new ServiceResult<>(false, "Lỗi: Không thể lưu tài khoản.", null);
        }
    }
}
