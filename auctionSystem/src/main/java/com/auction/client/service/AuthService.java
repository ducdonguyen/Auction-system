package com.auction.client.service;

import com.auction.client.network.SocketClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.auction.shared.models.AuthUser;
import com.auction.shared.network.LoginRequest;
import com.auction.shared.network.RegistrationRequest;
import com.auction.shared.network.ServiceResult;


public class AuthService {

    private static final Logger logger = LoggerFactory.getLogger(AuthService.class);

    public ServiceResult<AuthUser> login(LoginRequest request) {
        String username = (request.username() == null) ? "" : request.username().trim();
        String password = (request.password() == null) ? "" : request.password().trim();

        if (username.isEmpty() || password.isEmpty()) {
            return new ServiceResult<>(false, "Vui lòng nhập đầy đủ tên đăng nhập và mật khẩu.", null);
        }

        try {
            // Gửi gói tin Login lên Server
            SocketClient.getInstance().sendRequest(request);

            // Đợi Server phản hồi
            @SuppressWarnings("unchecked")
            ServiceResult<AuthUser> result = (ServiceResult<AuthUser>) SocketClient.getInstance().receiveResponse();

            // Nếu đăng nhập thành công, lưu thông tin phiên làm việc
            if (result.success() && result.data() != null) {
                SessionContext.setCurrentUser(result.data());
            }
            return result;
        } catch (Exception e) {
            logger.error("Lỗi kết nối mạng khi đăng nhập: {}", e.getMessage(), e);
            return new ServiceResult<>(false, "Lỗi kết nối mạng: Không thể kết nối tới Server.", null);
        }
    }

    public ServiceResult<AuthUser> register(RegistrationRequest request) {
        try {
            // Gửi gói tin Register lên Server
            SocketClient.getInstance().sendRequest(request);

            // Đợi Server phản hồi
            @SuppressWarnings("unchecked")
            ServiceResult<AuthUser> result = (ServiceResult<AuthUser>) SocketClient.getInstance().receiveResponse();

            return result;
        } catch (Exception e) {
            logger.error("Lỗi kết nối mạng khi đăng ký: {}", e.getMessage(), e);
            return new ServiceResult<>(false, "Lỗi kết nối mạng: Không thể kết nối tới Server.", null);
        }
    }
}
