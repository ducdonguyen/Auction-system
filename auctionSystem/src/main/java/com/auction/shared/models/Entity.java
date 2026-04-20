package com.auction.shared.models;

import java.io.Serializable;
import java.util.UUID;

public abstract class Entity implements Serializable {
    private static final long serialVersionUID = 1L;
    private String id;

    public Entity() {
        // Tự động sinh một chuỗi ID ngẫu nhiên không trùng lặp cho mọi đối tượng
        this.id = UUID.randomUUID().toString();
    }

    public String getId() {
        return id;
    }
}