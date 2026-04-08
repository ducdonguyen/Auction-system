package com.auction.common.model;

public class ItemFactory {
    public static Item createItem(String type, String name, String description, double price, String extraParam) {
        switch (type.toUpperCase()) {
            case "ELECTRONICS":
                return new Electronics(name, description, price, Integer.parseInt(extraParam));
            case "ART":

                return new Art(name, description, price, extraParam);
            case "VEHICLE":
                return new Vehicle(name, description, price, extraParam);
            default:
                throw new IllegalArgumentException("Loại sản phẩm không hợp lệ: " + type);
        }
    }
}