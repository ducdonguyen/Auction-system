package com.auction.shared.models;
public class Seller extends User {
    private static final long serialVersionUID = 1L;
    private double rating = 5.0;
    public Seller(String username, String password) { super(username, password); }
    public double getRating() { return rating; }
    public void setRating(double rating) { this.rating = rating; }
}