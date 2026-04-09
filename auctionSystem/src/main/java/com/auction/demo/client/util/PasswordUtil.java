package com.auction.demo.client.util;

import org.mindrot.jbcrypt.BCrypt;

public final class PasswordUtil {

    private PasswordUtil() {
    }

    public static String hashPassword(String plainPassword) {
        return BCrypt.hashpw(plainPassword, BCrypt.gensalt());
    }

    public static boolean matches(String plainPassword, String passwordHash) {
        return BCrypt.checkpw(plainPassword, passwordHash);
    }
}
