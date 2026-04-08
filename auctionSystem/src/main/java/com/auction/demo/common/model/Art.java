package com.auction.demo.common.model;

public class Art extends Item {
    private String author;

    public Art(String name, String description, double startingPrice, String author) {
        super(name, description, startingPrice);
        this.author = author;
    }

    public String getAuthor() {
        return author;
    }

    public void setAuthor(String author) {
        this.author = author;
    }
}