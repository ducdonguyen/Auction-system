package com.auction.shared.models;

public class Vehicle extends Item {
    private static final long serialVersionUID = 1L;
    private String brand;
    private int productionYear;

    public Vehicle(String name, String description, double startingPrice, String brand) {
        super(name, description, startingPrice);
        this.brand = brand;
    }

    public String getBrand() {
        return brand;
    }

    public void setBrand(String brand) {
        this.brand = brand;
    }
}
