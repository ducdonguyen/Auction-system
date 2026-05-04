package com.auction.shared.network;

public record LoginRequest(String username, String password) implements java.io.Serializable {
}
