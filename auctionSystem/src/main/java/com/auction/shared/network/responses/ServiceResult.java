package com.auction.shared.network.responses;

import java.io.Serializable;

/**
 * Kết quả của một yêu cầu xử lý dịch vụ.
 *
 * @param <T> Kiểu dữ liệu đi kèm kết quả.
 * @param success Trạng thái thành công hay thất bại.
 * @param message Thông báo đi kèm.
 * @param data Dữ liệu đi kèm.
 * @param serverTimeMillis Thời điểm server tạo response (epoch millis). Dùng để sync thời gian client với server.
 */
public record ServiceResult<T>(boolean success, String message, T data, long serverTimeMillis) implements Serializable {
}
