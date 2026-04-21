package com.auction.shared.models;

public class Art extends Item {
    private static final long serialVersionUID = 1L;
    private String author;

    public Art(String name, String description, double startingPrice, String author) {
        super(name, description, startingPrice);
        this.author = author;
    }

    public void setAuthor(String author) {
        this.author = author;
    }

    public String getItemType() { return "ART"; }
    public String getExtraInfo() { return author; }
}