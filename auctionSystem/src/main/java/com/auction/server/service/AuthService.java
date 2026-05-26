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

    /**
     * HÀM MỚI THÊM VÀO: Xử lý cộng dồn số tiền nạp vào tài khoản người dùng trong DB.
     * * @param username Định danh tài khoản cần nạp tiền.
     * @param amount   Số tiền yêu cầu nạp thêm.
     * @return Số dư mới sau khi đã nạp tiền thành công.
     * @throws Exception Các ngoại lệ xảy ra trong quá trình kiểm tra hoặc kết nối dữ liệu.
     */
    public double topUpBalance(String username, double amount) throws Exception {
        // 1. Kiểm tra tính hợp lệ của tham số đầu vào
        if (username == null || username.isBlank()) {
            throw new IllegalArgumentException("Định danh tài khoản không hợp lệ.");
        }
        if (amount <= 0) {
            throw new IllegalArgumentException("Số tiền nạp vào phải lớn hơn 0 đ.");
        }

        try {
            // 2. Xác thực tài khoản người dùng có tồn tại hay không
            AuthUser user = userDao.findByUsername(username);
            if (user == null) {
                throw new IllegalArgumentException("Tài khoản người dùng không tồn tại trên hệ thống.");
            }

            // 3. Thực hiện cập nhật số dư tăng thêm trực tiếp vào Database thông qua UserDao.
            // Phương thức updateBalance này sẽ trả về con số tổng (Số dư cũ + Số tiền mới nạp).
            double newBalance = userDao.updateBalance(username, amount);

            return newBalance;

        } catch (SQLException e) {
            e.printStackTrace();
            throw new Exception("Lỗi hệ thống khi tương tác với Cơ sở dữ liệu: " + e.getMessage());
        }
    }

    public double getBalance(String username) {
        try {
            return userDao.getBalance(username);
        } catch (SQLException e) {
            e.printStackTrace();
            return 0.0;
        }
    }

    public void freezeBalance(String username, double amount) {
        try {
            userDao.freezeBalance(username, amount);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void refundBalance(String username, double amount) {
        try {
            userDao.refundBalance(username, amount);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}