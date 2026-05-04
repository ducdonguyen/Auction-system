package com.auction.shared.network;

import java.io.Serializable;

/**
 * Lớp record đại diện cho kết quả của một yêu cầu xử lý dịch vụ.
 *
 * @param <T> Kiểu dữ liệu đi kèm kết quả.
 * @param success Trạng thái thành công hay thất bại.
 * @param message Thông báo đi kèm.
 * @param data Dữ liệu đi kèm.
 */
public record ServiceResult<T>(boolean success, String message, T data) implements Serializable {
}
