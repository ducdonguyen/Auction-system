package com.auction.shared.models;

import java.io.Serializable;

public class AuthUser implements Serializable {
    private static final long serialVersionUID = 1L;
    private Long id;
    private String fullName;
    private String username;
    private String email;
    private String passwordHash;
    private String role;
    private double balance;

    public AuthUser() {
        this.balance = 0.0;
    }

    // Constructor dùng khi Đăng ký (Chưa có ID từ CSDL)
    public AuthUser(String fullName, String username, String email, String passwordHash) {
        this.fullName = fullName;
        this.username = username;
        this.email = email;
        this.passwordHash = passwordHash;
        this.role = "USER";
        this.balance = 10000000.0; // Mặc định tặng 10tr để test
    }

    // Constructor dùng khi test hoặc khởi tạo nhanh (Có Role)
    public AuthUser(String fullName, String username, String email, String passwordHash, String role) {
        this.fullName = fullName;
        this.username = username;
        this.email = email;
        this.passwordHash = passwordHash;
        this.role = role;
        this.balance = 10000000.0;
    }

    // Constructor 6 tham số (Dùng cho các bản code cũ hơn)
    public AuthUser(Long id, String fullName, String username, String email, String passwordHash, String role) {
        this.id = id;
        this.fullName = fullName;
        this.username = username;
        this.email = email;
        this.passwordHash = passwordHash;
        this.role = role;
        this.balance = 10000000.0;
    }

    // Constructor 7 THAM SỐ ĐẦY ĐỦ (Dùng khi lôi từ CSDL lên, bao gồm cả ví tiền)
    public AuthUser(Long id, String fullName, String username, String email, String passwordHash, String role, double balance) {
        this.id = id;
        this.fullName = fullName;
        this.username = username;
        this.email = email;
        this.passwordHash = passwordHash;
        this.role = role;
        this.balance = balance;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public double getBalance() {
        return balance;
    }

    public void setBalance(double balance) {
        this.balance = balance;
    }

    @Override
    public String toString() {
        return "AuthUser{" +
                "id=" + id +
                ", username='" + username + '\'' +
                ", role='" + role + '\'' +
                ", balance=" + balance +
                '}';
    }
}
