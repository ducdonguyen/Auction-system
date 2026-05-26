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
    private double balance; // THÊM MỚI: Thuộc tính quản lý số dư ví tiền tài khoản

    public AuthUser() {
    }

    // Constructor dùng khi Đăng ký (Chưa có ID từ CSDL, số dư mặc định = 0.0)
    public AuthUser(String fullName, String username, String email, String passwordHash) {
        this.fullName = fullName;
        this.username = username;
        this.email = email;
        this.passwordHash = passwordHash;
        this.role = "USER";
        this.balance = 0.0;
    }

    // Constructor dùng khi test hoặc khởi tạo nhanh (Có Role, số dư mặc định = 0.0)
    public AuthUser(String fullName, String username, String email, String passwordHash, String role) {
        this.fullName = fullName;
        this.username = username;
        this.email = email;
        this.passwordHash = passwordHash;
        this.role = role;
        this.balance = 0.0;
    }

    // Constructor ĐẦY ĐỦ NHẤT (Dùng khi lôi từ CSDL lên, có cả ID và số dư tài khoản)
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

    // THÊM MỚI: Getter cho balance
    public double getBalance() {
        return balance;
    }

    // THÊM MỚI: Setter cho balance
    public void setBalance(double balance) {
        this.balance = balance;
    }

    @Override
    public String toString() {
        return "AuthUser{" +
                "id=" + id +
                ", username='" + username + '\'' +
                ", role='" + role + '\'' +
                ", balance=" + balance + // Cập nhật toString hiển thị kèm số dư tiện cho việc log debug
                '}';
    }
}