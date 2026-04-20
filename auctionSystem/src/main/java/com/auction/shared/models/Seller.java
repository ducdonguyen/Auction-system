package com.auction.shared.models;

public class Seller extends User {
    private static final long serialVersionUID = 1L;
    private double rating; // Đánh giá uy tín (ví dụ: từ 1.0 đến 5.0)

    public Seller(String username, String password) {
        super(username, password);
        this.rating = 5.0; // Mặc định người bán mới có uy tín tối đa
    }

    public double getRating() {
        return rating;
    }

    public void setRating(double rating) {
        this.rating = rating;
    }
}