package com.auction.demo.client.model;
public record RegistrationRequest(String fullName, String username, String email, String password, String confirmPassword) {}
