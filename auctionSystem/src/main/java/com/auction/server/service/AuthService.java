package com.auction.server.service;

import com.auction.server.dao.UserDao;
import com.auction.server.util.PasswordUtil;
import com.auction.shared.models.AuthUser;
import com.auction.shared.network.LoginRequest;
import com.auction.shared.network.RegistrationRequest;
import com.auction.shared.network.ServiceResult;
import java.sql.SQLException;

public class AuthService {
    private final UserDao userDao = new UserDao();

    public ServiceResult<AuthUser> login(LoginRequest request) {
        // Kiểm tra dữ liệu đầu vào cơ bản
        if (request.username() == null || request.username().isBlank() ||
                request.password() == null || request.password().isBlank()) {
            return new ServiceResult<>(false, "Vui lòng nhập đủ thông tin.", null);
        }

        try {
            // Xuống DB tìm User theo username (Đã lấy kèm cột account_role)
            AuthUser user = userDao.findByUsername(request.username());

            if (user == null) {
                return new ServiceResult<>(false, "Tài khoản không tồn tại.", null);
            }

            if (PasswordUtil.matches(request.password(), user.getPasswordHash())) {
                user.setPasswordHash(null);

                return new ServiceResult<>(true, "Đăng nhập thành công!", user);
            } else {
                return new ServiceResult<>(false, "Mật khẩu không chính xác.", null);
            }

        } catch (SQLException e) {
            e.printStackTrace();
            return new ServiceResult<>(false, "Lỗi kết nối cơ sở dữ liệu trên Server.", null);
        }
    }

    public ServiceResult<AuthUser> register(RegistrationRequest request) {
        // Kiểm tra dữ liệu đầu vào cơ bản
        if (request.username() == null || request.username().isBlank() ||
                request.password() == null || request.password().isBlank() ||
                request.email() == null || request.email().isBlank()) {
            return new ServiceResult<>(false, "Vui lòng nhập đầy đủ thông tin đăng ký.", null);
        }

        try {
            // Kiểm tra trùng lặp
            if (userDao.existsByUsernameOrEmail(request.username(), request.email())) {
                return new ServiceResult<>(false, "Tên đăng nhập hoặc Email đã tồn tại.", null);
            }

            AuthUser newUser = new AuthUser(
                    request.fullName(),
                    request.username(),
                    request.email(),
                    PasswordUtil.hashPassword(request.password())
            );

            // Lưu xuống DB
            userDao.register(newUser);

            // Xóa mật khẩu trước khi trả về
            newUser.setPasswordHash(null);
            return new ServiceResult<>(true, "Đăng ký tài khoản thành công!", newUser);

        } catch (SQLException e) {
            e.printStackTrace();
            return new ServiceResult<>(false, "Lỗi kết nối cơ sở dữ liệu khi đăng ký.", null);
        }
    }
}