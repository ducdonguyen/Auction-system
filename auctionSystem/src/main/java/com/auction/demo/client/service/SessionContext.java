package com.auction.demo.client.service;
import com.auction.shared.models.AuthUser;
public class SessionContext {
    private static AuthUser currentUser;
    public static AuthUser getCurrentUser() { return currentUser; }
    public static void setCurrentUser(AuthUser user) { currentUser = user; }
}
