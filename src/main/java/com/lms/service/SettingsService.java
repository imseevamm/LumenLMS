package com.lms.service;

import com.lms.dao.SettingsDAO;
import com.lms.model.Role;
import com.lms.util.SessionManager;
import com.lms.util.ValidationUtil;

import java.util.Map;

/** Business rules for platform-wide settings. Reading is open; changing settings is administrator-only. */
public class SettingsService {

    private final SettingsDAO settingsDAO = new SettingsDAO();

    public static class SettingsException extends RuntimeException {
        public SettingsException(String message) { super(message); }
    }

    public Map<String, String> getAll() {
        return settingsDAO.findAll();
    }

    /** Validates and persists the general settings. Throws AuthorizationException for non-admins. */
    public void saveGeneral(String platformName, String supportEmail, boolean allowRegistration) {
        SessionManager.getInstance().requireRole(Role.ADMIN);
        if (!ValidationUtil.isNotBlank(platformName)) throw new SettingsException("Platform name is required.");
        if (!ValidationUtil.isValidEmail(supportEmail)) throw new SettingsException("Please enter a valid support email address.");
        settingsDAO.set("platform_name", platformName.trim());
        settingsDAO.set("support_email", supportEmail.trim().toLowerCase());
        settingsDAO.set("allow_registration", String.valueOf(allowRegistration));
        settingsDAO.set("default_theme", "light");
    }
}
