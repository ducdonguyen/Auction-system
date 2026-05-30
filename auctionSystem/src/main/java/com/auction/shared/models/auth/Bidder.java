package com.auction.shared.models.auth;

/**
 * Lớp đại diện cho người đặt thầu.
 */
public class Bidder extends User {
  private static final long serialVersionUID = 1L;
  private double balance;

  /**
   * Khởi tạo người đặt thầu.
   *
   * @param username Tên đăng nhập.
   * @param password Mật khẩu.
   * @param balance  Số dư tài khoản.
   */
  public Bidder(String username, String password, double balance) {
    super(username, password);
    this.balance = balance;
  }

  public double getBalance() {
    return balance;
  }

  public void setBalance(double balance) {
    this.balance = balance;
  }

  /**
   * Nạp tiền vào tài khoản.
   *
   * @param amount Số tiền nạp.
   */
  public void deposit(double amount) {
    if (amount > 0) {
      balance += amount;
    }
  }
}