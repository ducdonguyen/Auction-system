package com.auction.client.service;

import com.auction.shared.models.auth.UserAccount;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class SessionContextTest {

    @Test
    @DisplayName("Kiểm thử SessionContext")
    void testSessionContext() {
        UserAccount user = new UserAccount("Name", "user", "email", "hash", "USER");
        SessionContext.setCurrentUser(user);
        assertEquals(user, SessionContext.getCurrentUser());
        
        SessionContext.setCurrentUser(null);
        assertNull(SessionContext.getCurrentUser());
    }
}
