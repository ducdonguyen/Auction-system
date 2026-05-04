package com.auction.shared.models;

import java.util.HashMap;
import java.util.Map;

public class ItemFactory {

    // Khai báo một giao diện chức năng để tạo Item
    @FunctionalInterface
    public interface ItemCreator {
        Item create(String name, String description, double price, String extraParam);
    }

    // Sổ đăng ký (Registry) chứa các công thức tạo sản phẩm
    private static final Map<String, ItemCreator> ITEM_REGISTRY = new HashMap<>();

    // Khởi tạo các công thức mặc định một lần duy nhất
    static {
        ITEM_REGISTRY.put("ELECTRONICS", (name, description, price, extraParam) ->
                new Electronics(name, description, price, Integer.parseInt(extraParam)));
        ITEM_REGISTRY.put("ART", (name, description, price, extraParam) ->
                new Art(name, description, price, extraParam));
        ITEM_REGISTRY.put("VEHICLE", (name, description, price, extraParam) ->
                new Vehicle(name, description, price, extraParam));
    }

    /**
     * Cho phép các Module khác tự do thêm loại sản phẩm mới từ bên ngoài
     */
    public static void registerNewItemType(String type, ItemCreator creator) {
        ITEM_REGISTRY.put(type.toUpperCase(), creator);
    }

    public static Item createItem(String type, String name, String description, double price, String extraParam) {
        ItemCreator creator = ITEM_REGISTRY.get(type.toUpperCase());
        if (creator == null) {
            throw new IllegalArgumentException("Loại sản phẩm không hợp lệ: " + type);
        }
        return creator.create(name, description, price, extraParam);
    }
}