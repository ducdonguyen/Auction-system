package com.auction.common.model;

public class Vehicle extends Item {
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
