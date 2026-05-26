package com.auction.client.service;

import com.auction.shared.models.AuthUser;

/**
 * Lưu trữ thông tin người dùng đang đăng nhập trong phiên làm việc hiện tại.
 */
public class SessionContext {
  private SessionContext() {
  }

  private static volatile AuthUser currentUser;

  public static synchronized AuthUser getCurrentUser() {
    return currentUser;
  }

  public static synchronized void setCurrentUser(AuthUser user) {
    currentUser = user;
  }
}
