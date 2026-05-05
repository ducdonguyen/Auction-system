package com.auction.shared.models;

/**
 * Lớp trừu tượng đại diện cho một sản phẩm đấu giá.
 */
public abstract class Item extends Entity {
  private static final long serialVersionUID = 1L;
  private String name;
  private String description;
  private double startingPrice;

  /**
   * Khởi tạo sản phẩm đấu giá.
   *
   * @param name          Tên sản phẩm.
   * @param description   Mô tả sản phẩm.
   * @param startingPrice Giá khởi điểm.
   */
  public Item(String name, String description, double startingPrice) {
    this.name = name;
    this.description = description;
    this.startingPrice = startingPrice;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getDescription() {
    return description;
  }

  public void setDescription(String description) {
    this.description = description;
  }

  public double getStartingPrice() {
    return startingPrice;
  }

  public void setStartingPrice(double startingPrice) {
    this.startingPrice = startingPrice;
  }

  public abstract String getItemType();

  public abstract String getExtraInfo();
}