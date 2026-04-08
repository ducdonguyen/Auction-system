package com.auction.common.model;

public class Bidder extends User {
    private double balance;

    public Bidder(String username, String password, double initialBalance) {
        // Gọi constructor của lớp cha (User) để khởi tạo username và password
        super(username, password);
        this.balance = initialBalance;
    }

    public double getBalance() {
        return balance;
    }

    public void setBalance(double balance) {
        this.balance = balance;
    }

    public void deposit(double amount) {
        if (amount > 0) {
            this.balance += amount;
        }
    }
}