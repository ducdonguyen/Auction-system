package com.auction.client.service;

import com.auction.client.network.SocketClient;
import com.auction.shared.models.auth.UserAccount;
import com.auction.shared.network.requests.LoginRequest;
import com.auction.shared.network.requests.RegistrationRequest;
import com.auction.shared.network.responses.ServiceResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Service xử lý các nghiệp vụ liên quan đến xác thực (đăng nhập, đăng ký).
 */
public class AuthService {
  private static final Logger logger = LoggerFactory.getLogger(AuthService.class);

  /**
   * Thực hiện yêu cầu đăng nhập.
   *
   * @param request Yêu cầu đăng nhập.
   * @return Kết quả đăng nhập.
   */
  @SuppressWarnings("unchecked")
  public ServiceResult<UserAccount> login(LoginRequest request) {
    if (request.username() == null || request.username().isBlank()
        || request.password() == null || request.password().isBlank()) {
      return new ServiceResult<>(false, "Nhập đủ thông tin.", null);
    }
    try {
      SocketClient.getInstance().sendRequest(request);
      ServiceResult<UserAccount> result =
          (ServiceResult<UserAccount>) SocketClient.getInstance().receiveResponse();
      if (result.success() && result.data() != null) {
        SessionContext.setCurrentUser(result.data());
      }
      return result;
    } catch (Exception e) {
      logger.error("Login error: {}", e.getMessage());
      return new ServiceResult<>(false, "Lỗi kết nối.", null);
    }
  }

  /**
   * Thực hiện yêu cầu đăng ký.
   *
   * @param request Yêu cầu đăng ký.
   * @return Kết quả đăng ký.
   */
  @SuppressWarnings("unchecked")
  public ServiceResult<UserAccount> register(RegistrationRequest request) {
    try {
      SocketClient.getInstance().sendRequest(request);
      return (ServiceResult<UserAccount>) SocketClient.getInstance().receiveResponse();
    } catch (Exception e) {
      logger.error("Reg error: {}", e.getMessage());
      return new ServiceResult<>(false, "Lỗi kết nối.", null);
    }
  }
}
