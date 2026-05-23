package com.auction.shared.models;

/**
 * Lớp đại diện cho các sản phẩm đấu giá thuộc loại "Khác" (Không nằm trong các danh mục cố định).
 */
public class GenericItem extends Item {
    private static final long serialVersionUID = 1L;

    private String details;

    /**
     * Khởi tạo sản phẩm loại Khác.
     *
     * @param name          Tên sản phẩm.
     * @param description   Mô tả chi tiết.
     * @param startingPrice Giá khởi điểm.
     * @param details       Chi tiết loại sản phẩm (VD: Bất động sản, Thú cưng...).
     */
    public GenericItem(String name, String description, double startingPrice, String details) {
        super(name, description, startingPrice);
        this.details = details;
    }

    public String getDetails() {
        return details;
    }

    public void setDetails(String details) {
        this.details = details;
    }

    public String getItemType() {
        return "OTHER";
    }

    public String getExtraInfo() {
        return details;
    }
}