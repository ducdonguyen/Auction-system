package com.auction.client.service;

import com.auction.shared.models.auth.UserAccount;

/**
 * Lưu trữ thông tin người dùng đang đăng nhập trong phiên làm việc hiện tại.
 */
public class SessionContext {
  private SessionContext() {
  }

  private static volatile UserAccount currentUser;

  public static synchronized UserAccount getCurrentUser() {
    return currentUser;
  }

  public static synchronized void setCurrentUser(UserAccount user) {
    currentUser = user;
  }
}
