package com.auction.server.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class PasswordUtilTest {

    @Test
    @DisplayName("Kiểm thử băm và kiểm tra mật khẩu")
    void testHashAndMatch() {
        String password = "mySecretPassword123";
        String hash = PasswordUtil.hashPassword(password);

        assertNotNull(hash);
        assertNotEquals(password, hash);
        assertTrue(PasswordUtil.matches(password, hash));
        assertFalse(PasswordUtil.matches("wrongPassword", hash));
    }
}
