package com.lms.util;

import com.lms.dao.SettingsDAO;

import java.util.Map;

/** Central access point for platform-wide settings used by the UI and services. */
public final class AppSettings {
    private static final SettingsDAO DAO = new SettingsDAO();
    private static final String DEFAULT_NAME = "LumenLMS";
    private static final String DEFAULT_SUPPORT = "support@lumenlms.com";
    private static final String DEFAULT_THEME = "light";

    private AppSettings() {}

    private static Map<String, String> load() {
        try {
            return DAO.findAll();
        } catch (RuntimeException ex) {
            return Map.of();
        }
    }

    public static String platformName() {
        String value = load().get("platform_name");
        return value == null || value.isBlank() ? DEFAULT_NAME : value.trim();
    }

    public static String supportEmail() {
        String value = load().get("support_email");
        return value == null || value.isBlank() ? DEFAULT_SUPPORT : value.trim();
    }

    public static String theme() {
        return DEFAULT_THEME;
    }
}
