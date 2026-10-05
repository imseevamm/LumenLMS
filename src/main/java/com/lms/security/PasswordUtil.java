package com.lms.security;

import org.mindrot.jbcrypt.BCrypt;

/**
 * Handles all password hashing/verification. Passwords are never stored or compared in plain text.
 * Uses BCrypt (adaptive, salted hashing) rather than a fast general-purpose hash like MD5/SHA-256,
 * which would be unsuitable for password storage.
 */
public final class PasswordUtil {

    private static final int WORK_FACTOR = 12;

    private PasswordUtil() {}

    public static String hash(String plainPassword) {
        return BCrypt.hashpw(plainPassword, BCrypt.gensalt(WORK_FACTOR));
    }

    public static boolean verify(String plainPassword, String hashedPassword) {
        try {
            return BCrypt.checkpw(plainPassword, hashedPassword);
        } catch (IllegalArgumentException e) {
            // hash was malformed / not a bcrypt hash
            return false;
        }
    }

    /** Returns a 0-4 strength score used to drive the UI strength indicator. */
    public static int strengthScore(String password) {
        if (password == null || password.isEmpty()) return 0;
        int score = 0;
        if (password.length() >= 8) score++;
        if (password.matches(".*[A-Z].*")) score++;
        if (password.matches(".*[a-z].*") && password.matches(".*\\d.*")) score++;
        if (password.matches(".*[!@#$%^&*()\\-_=+{};:,<.>].*")) score++;
        return Math.min(score, 4);
    }
}
