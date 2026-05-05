package com.auction.shared.models;

/**
 * Lớp đại diện cho sản phẩm điện tử.
 */
public class Electronics extends Item {
  private static final long serialVersionUID = 1L;
  private int warrantyMonths;

  /**
   * Khởi tạo sản phẩm điện tử.
   *
   * @param name           Tên sản phẩm.
   * @param description    Mô tả.
   * @param startingPrice  Giá khởi điểm.
   * @param warrantyMonths Thời hạn bảo hành (tháng).
   */
  public Electronics(String name, String description, double startingPrice, int warrantyMonths) {
    super(name, description, startingPrice);
    this.warrantyMonths = warrantyMonths;
  }

  public void setWarrantyMonths(int warrantyMonths) {
    this.warrantyMonths = warrantyMonths;
  }

  public String getItemType() {
    return "ELECTRONICS";
  }

  public String getExtraInfo() {
    return String.valueOf(warrantyMonths);
  }
}