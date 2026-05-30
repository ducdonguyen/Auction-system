package com.auction.shared.network.requests;

public record RegistrationRequest(String fullName, String username, String email, String password)
        implements java.io.Serializable {
}
