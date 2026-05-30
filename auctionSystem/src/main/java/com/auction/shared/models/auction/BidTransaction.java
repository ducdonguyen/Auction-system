package com.auction.shared.models.auction;

import com.auction.shared.models.auth.Bidder;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * Lớp record đại diện cho một giao dịch đặt thầu.
 *
 * @param transactionId ID giao dịch.
 * @param bidder        Người đặt thầu.
 * @param bidAmount     Số tiền thầu.
 * @param timestamp     Thời điểm đặt thầu.
 */
public record BidTransaction(String transactionId, Bidder bidder, double bidAmount,
                             LocalDateTime timestamp) implements Serializable {
  private static final long serialVersionUID = 1L;
}