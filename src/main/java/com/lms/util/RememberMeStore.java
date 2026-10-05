package com.lms.util;

import com.lms.model.Role;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.prefs.Preferences;

/** Stores remembered login credentials locally using authenticated encryption. */
public final class RememberMeStore {
    private static final Preferences PREFS = Preferences.userNodeForPackage(RememberMeStore.class);
    private static final String EMAIL_KEY = "remembered_email_v3";
    private static final String PASSWORD_KEY = "remembered_password_v3";
    private static final String ROLE_KEY = "remembered_role_v3";
    private static final String LEGACY_PASSWORD_KEY = "remembered_password_v2";
    private static final SecureRandom RANDOM = new SecureRandom();

    static {
        PREFS.remove(LEGACY_PASSWORD_KEY);
    }

    private RememberMeStore() {}

    public static String email() {
        return PREFS.get(EMAIL_KEY, "");
    }

    public static String password() {
        String encoded = PREFS.get(PASSWORD_KEY, "");
        if (encoded.isBlank()) return "";
        try {
            byte[] packed = Base64.getDecoder().decode(encoded);
            byte[] iv = new byte[12];
            byte[] cipherText = new byte[packed.length - iv.length];
            System.arraycopy(packed, 0, iv, 0, iv.length);
            System.arraycopy(packed, iv.length, cipherText, 0, cipherText.length);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE, key(), new GCMParameterSpec(128, iv));
            return new String(cipher.doFinal(cipherText), StandardCharsets.UTF_8);
        } catch (Exception ex) {
            clear();
            return "";
        }
    }

    public static boolean hasRememberedLogin() {
        return !email().isBlank() && !password().isBlank();
    }

    public static void save(String email, String password, Role role) {
        PREFS.put(EMAIL_KEY, email == null ? "" : email.trim().toLowerCase());
        PREFS.put(PASSWORD_KEY, encrypt(password == null ? "" : password));
        PREFS.put(ROLE_KEY, role == null ? Role.STUDENT.name() : role.name());
    }

    public static Role role() {
        try {
            return Role.valueOf(PREFS.get(ROLE_KEY, Role.STUDENT.name()));
        } catch (IllegalArgumentException ex) {
            return Role.STUDENT;
        }
    }

    public static void clear() {
        PREFS.remove(EMAIL_KEY);
        PREFS.remove(PASSWORD_KEY);
        PREFS.remove(LEGACY_PASSWORD_KEY);
        PREFS.remove(ROLE_KEY);
    }

    private static String encrypt(String value) {
        try {
            byte[] iv = new byte[12];
            RANDOM.nextBytes(iv);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE, key(), new GCMParameterSpec(128, iv));
            byte[] cipherText = cipher.doFinal(value.getBytes(StandardCharsets.UTF_8));
            byte[] packed = new byte[iv.length + cipherText.length];
            System.arraycopy(iv, 0, packed, 0, iv.length);
            System.arraycopy(cipherText, 0, packed, iv.length, cipherText.length);
            return Base64.getEncoder().encodeToString(packed);
        } catch (Exception ex) {
            throw new IllegalStateException("Unable to protect remembered credentials.", ex);
        }
    }

    private static SecretKeySpec key() throws Exception {
        String material = System.getProperty("user.name", "unknown") + "|LumenLMS-RememberMe-v3";
        byte[] digest = MessageDigest.getInstance("SHA-256")
                .digest(material.getBytes(StandardCharsets.UTF_8));
        return new SecretKeySpec(digest, "AES");
    }
}
