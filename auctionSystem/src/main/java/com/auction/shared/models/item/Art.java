package com.auction.shared.models.item;

/**
 * Lớp đại diện cho sản phẩm nghệ thuật.
 */
public class Art extends Item {
  private static final long serialVersionUID = 1L;
  private String author;

  /**
   * Khởi tạo sản phẩm nghệ thuật.
   *
   * @param name          Tên sản phẩm.
   * @param description   Mô tả.
   * @param startingPrice Giá khởi điểm.
   * @param author        Tác giả.
   */
  public Art(String name, String description, double startingPrice, String author) {
    super(name, description, startingPrice);
    this.author = author;
  }

  public void setAuthor(String author) {
    this.author = author;
  }

  public String getItemType() {
    return "ART";
  }

  public String getExtraInfo() {
    return author;
  }
}