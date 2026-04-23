package com.auction.shared.network;

public record ServiceResult<T>(boolean success, String message, T data) implements java.io.Serializable {
}
