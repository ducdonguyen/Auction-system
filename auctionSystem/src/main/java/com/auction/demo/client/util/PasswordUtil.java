package com.auction.demo.client.util;
import org.mindrot.jbcrypt.BCrypt;
public class PasswordUtil {
    public static String hashPassword(String p) { return BCrypt.hashpw(p, BCrypt.gensalt()); }
    public static boolean matches(String p, String h) { return BCrypt.checkpw(p, h); }
}
