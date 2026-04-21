package com.auction.shared.models;

public class Electronics extends Item {
    private static final long serialVersionUID = 1L;
    private int warrantyMonths;

    public Electronics(String name, String description, double startingPrice, int warrantyMonths) {
        super(name, description, startingPrice);
        this.warrantyMonths = warrantyMonths;
    }

    public void setWarrantyMonths(int warrantyMonths) {
        this.warrantyMonths = warrantyMonths;
    }

    public String getItemType() { return "ELECTRONICS"; }
    public String getExtraInfo() { return String.valueOf(warrantyMonths); }
}