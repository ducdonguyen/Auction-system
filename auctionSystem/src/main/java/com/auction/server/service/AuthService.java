package com.auction.server.service;

import com.auction.server.dao.UserDao;
import com.auction.server.util.PasswordUtil;
import com.auction.shared.models.auth.UserAccount;
import com.auction.shared.network.requests.LoginRequest;
import com.auction.shared.network.requests.RegistrationRequest;

import com.auction.shared.network.responses.ServiceResult;

import java.sql.SQLException;



public class AuthService {
    /**
     * Retrieves the full name of a user by username.
     * @param username the user's username
     * @return the full name or null if user not found or on error
     */
    public String getFullName(String username) {
        try {
            UserAccount user = userDao.findByUsername(username);
            return user != null ? user.getFullName() : null;
        } catch (SQLException e) {
            e.printStackTrace();
            return null;
        }
    }

    private UserDao userDao = new UserDao();

    public AuthService() {
        this.userDao = new UserDao();
    }


    public AuthService(UserDao userDao) {
        this.userDao = userDao;
    }
    public ServiceResult<UserAccount> login(LoginRequest request) {
        // Kiểm tra dữ liệu đầu vào cơ bản
        if (request.username() == null || request.username().isBlank() ||
                request.password() == null || request.password().isBlank()) {
            return new ServiceResult<>(false, "Vui lòng nhập đủ thông tin.", null, System.currentTimeMillis());
        }

        try {
            // Xuống DB tìm User theo username (Đã lấy kèm cột account_role)
            UserAccount user = userDao.findByUsername(request.username());

            if (user == null) {
                return new ServiceResult<>(false, "Tài khoản không tồn tại.", null, System.currentTimeMillis());
            }

            if (PasswordUtil.matches(request.password(), user.getPasswordHash())) {
                user.setPasswordHash(null);

                return new ServiceResult<>(true, "Đăng nhập thành công!", user, System.currentTimeMillis());
            } else {
                return new ServiceResult<>(false, "Mật khẩu không chính xác.", null, System.currentTimeMillis());
            }

        } catch (SQLException e) {
            e.printStackTrace();
            return new ServiceResult<>(false, "Lỗi kết nối cơ sở dữ liệu trên Server.", null, System.currentTimeMillis());
        }
    }

    public ServiceResult<UserAccount> register(RegistrationRequest request) {
        // Kiểm tra dữ liệu đầu vào cơ bản
        if (request.username() == null || request.username().isBlank() ||
                request.password() == null || request.password().isBlank() ||
                request.email() == null || request.email().isBlank()) {
            return new ServiceResult<>(false, "Vui lòng nhập đầy đủ thông tin đăng ký.", null, System.currentTimeMillis());
        }

        try {
            // Kiểm tra trùng lặp
            if (userDao.existsByUsernameOrEmail(request.username(), request.email())) {
                return new ServiceResult<>(false, "Tên đăng nhập hoặc Email đã tồn tại.", null, System.currentTimeMillis());
            }

            UserAccount newUser = new UserAccount(
                    null,
                    request.fullName(),
                    request.username(),
                    request.email(),
                    PasswordUtil.hashPassword(request.password()),
                    "USER",
                    0.0
            );

            // Lưu xuống DB
            userDao.register(newUser);

            // Xóa mật khẩu trước khi trả về
            newUser.setPasswordHash(null);
            return new ServiceResult<>(true, "Đăng ký tài khoản thành công!", newUser, System.currentTimeMillis());

        } catch (SQLException e) {
            e.printStackTrace();
            return new ServiceResult<>(false, "Lỗi kết nối cơ sở dữ liệu khi đăng ký.", null, System.currentTimeMillis());
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
            throw new IllegalArgumentException("Số tiền nạp không hợp lệ");
        }

        try {
            // 2. Xác thực tài khoản người dùng có tồn tại hay không
            UserAccount user = userDao.findByUsername(username);
            if (user == null) {
                throw new IllegalArgumentException("Tài khoản người dùng không tồn tại trên hệ thống.");
            }

            // 3. Thực hiện cập nhật số dư tăng thêm trực tiếp vào Database thông qua UserDao.
            // Phương thức updateBalance này sẽ trả về con số tổng (Số dư cũ + Số tiền mới nạp).
            return userDao.updateBalance(username, amount);

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

    public boolean freezeBalance(String username, double amount) {
        try {
            return userDao.freezeBalance(username, amount);
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
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