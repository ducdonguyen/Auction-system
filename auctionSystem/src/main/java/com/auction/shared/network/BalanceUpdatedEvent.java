package com.auction.shared.network;

import java.io.Serializable;

/**
 * Sự kiện thông báo số dư tài khoản của người dùng đã thay đổi.
 * @param newBalance Số dư mới sau khi thay đổi.
 * @param amountChanged Số tiền thay đổi (số dương là cộng vào/hoàn tiền).
 * @param reason Lý do thay đổi.
 */
public record BalanceUpdatedEvent(double newBalance, double amountChanged, String reason) implements Serializable {
}
