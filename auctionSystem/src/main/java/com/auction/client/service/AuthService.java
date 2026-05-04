package com.auction.client.service;
import com.auction.client.network.SocketClient;
import org.slf4j.*;
import com.auction.shared.models.AuthUser;
import com.auction.shared.network.*;
public class AuthService {
    private static final Logger logger = LoggerFactory.getLogger(AuthService.class);
    @SuppressWarnings("unchecked")
    public ServiceResult<AuthUser> login(LoginRequest request) {
        if (request.username() == null || request.username().isBlank() || request.password() == null || request.password().isBlank())
            return new ServiceResult<>(false, "Nhập đủ thông tin.", null);
        try {
            SocketClient.getInstance().sendRequest(request);
            ServiceResult<AuthUser> result = (ServiceResult<AuthUser>) SocketClient.getInstance().receiveResponse();
            if (result.success() && result.data() != null) SessionContext.setCurrentUser(result.data());
            return result;
        } catch (Exception e) { logger.error("Login error: {}", e.getMessage()); return new ServiceResult<>(false, "Lỗi kết nối.", null); }
    }
    @SuppressWarnings("unchecked")
    public ServiceResult<AuthUser> register(RegistrationRequest request) {
        try {
            SocketClient.getInstance().sendRequest(request);
            return (ServiceResult<AuthUser>) SocketClient.getInstance().receiveResponse();
        } catch (Exception e) { logger.error("Reg error: {}", e.getMessage()); return new ServiceResult<>(false, "Lỗi kết nối.", null); }
    }
}
