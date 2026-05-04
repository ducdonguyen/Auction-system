package com.auction.shared.models;

public class Bidder extends User {
    private static final long serialVersionUID = 1L;
    private double balance;

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

    public void deposit(double amount) {
        if (amount > 0) {
            balance += amount;
        };
    }
}