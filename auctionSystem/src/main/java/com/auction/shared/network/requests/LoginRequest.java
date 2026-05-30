package com.auction.shared.network.requests;

public record LoginRequest(String username, String password) implements java.io.Serializable {
}
