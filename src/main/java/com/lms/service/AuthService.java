package com.lms.service;

import com.lms.dao.NotificationDAO;
import com.lms.dao.SettingsDAO;
import com.lms.dao.UserDAO;
import com.lms.model.Notification;
import com.lms.model.Role;
import com.lms.model.User;
import com.lms.security.PasswordUtil;
import com.lms.util.SessionManager;
import com.lms.util.AppSettings;
import com.lms.util.ValidationUtil;

import java.util.Optional;

/**
 * Handles authentication and account-creation business rules.
 * Critically: this is the ONLY entry point for account creation, and it enforces that
 * ordinary sign-up can never create an ADMIN account - admins are provisioned separately
 * by an existing administrator via UserService.
 */
public class AuthService {

    private final UserDAO userDAO = new UserDAO();
    private final NotificationDAO notificationDAO = new NotificationDAO();
    private final SettingsDAO settingsDAO = new SettingsDAO();

    public static class AuthException extends RuntimeException {
        public AuthException(String message) { super(message); }
    }

    /** Attempts a login; throws AuthException with a user-friendly message on any failure. */
    public User login(String email, String password) {
        return login(email, password, null);
    }

    /**
     * Authenticates a user and, when an expected role is supplied, verifies that
     * the selected login path matches the account role before creating a session.
     */
    public User login(String email, String password, Role expectedRole) {
        if (!ValidationUtil.isNotBlank(email) || !ValidationUtil.isNotBlank(password)) {
            throw new AuthException("Please enter both email and password.");
        }
        Optional<User> found = userDAO.findByEmail(email.trim().toLowerCase());
        if (found.isEmpty() || !PasswordUtil.verify(password, found.get().getPasswordHash())) {
            throw new AuthException("Invalid email or password.");
        }
        User user = found.get();
        if (!user.isActive()) {
            throw new AuthException("This account has been deactivated. Contact the administrator.");
        }
        if (expectedRole != null && user.getRole() != expectedRole) {
            throw new AuthException("This account is registered as " + user.getRole().display() + ". Please select the correct login type.");
        }
        SessionManager.getInstance().setCurrentUser(user);
        return user;
    }

    /**
     * Public self-registration. Role is restricted to STUDENT or INSTRUCTOR only -
     * ADMIN is intentionally excluded from the allowed set for this method's signature usage,
     * and is defensively rejected again below even if a caller attempts to bypass the UI.
     */
    public User signup(String fullName, String email, String password, String confirmPassword, Role requestedRole) {
        if (!"true".equalsIgnoreCase(settingsDAO.findAll().getOrDefault("allow_registration", "true"))) {
            throw new AuthException("Public registration is currently disabled by the administrator.");
        }
        if (!ValidationUtil.isNotBlank(fullName)) throw new AuthException("Full name is required.");
        if (!ValidationUtil.isValidEmail(email)) throw new AuthException("Please enter a valid email address.");
        if (!ValidationUtil.isStrongEnough(password)) {
            throw new AuthException("Password must be at least 8 characters and include a letter and a number.");
        }
        if (!ValidationUtil.passwordsMatch(password, confirmPassword)) {
            throw new AuthException("Passwords do not match.");
        }
        if (requestedRole == Role.ADMIN) {
            throw new AuthException("Admin accounts cannot be created through sign-up.");
        }
        if (userDAO.findByEmail(email.trim().toLowerCase()).isPresent()) {
            throw new AuthException("An account with this email already exists.");
        }

        User user = new User();
        user.setFullName(fullName.trim());
        user.setEmail(email.trim().toLowerCase());
        user.setPasswordHash(PasswordUtil.hash(password));
        user.setRole(requestedRole);
        user.setActive(true);
        User saved = userDAO.insert(user);

        Notification welcome = new Notification();
        welcome.setUserId(saved.getId());
        welcome.setTitle("Welcome to " + AppSettings.platformName() + "!");
        welcome.setMessage(requestedRole == Role.STUDENT
                ? "Your student account has been created. Roll number: " + saved.getRollNumber() + "."
                : "Your instructor account has been created successfully.");
        welcome.setType(Notification.Type.SUCCESS);
        notificationDAO.insert(welcome);

        return saved;
    }

    public void changePassword(long userId, String currentPassword, String newPassword, String confirmPassword) {
        User caller = SessionManager.getInstance().getCurrentUser();
        if (caller == null || caller.getId() != userId) {
            throw new com.lms.security.AuthorizationException("You can only change your own password.");
        }
        User user = userDAO.findById(userId).orElseThrow(() -> new AuthException("User not found."));
        if (!PasswordUtil.verify(currentPassword, user.getPasswordHash())) {
            throw new AuthException("Current password is incorrect.");
        }
        if (!ValidationUtil.isStrongEnough(newPassword)) {
            throw new AuthException("New password must be at least 8 characters and include a letter and a number.");
        }
        if (!ValidationUtil.passwordsMatch(newPassword, confirmPassword)) {
            throw new AuthException("New passwords do not match.");
        }
        userDAO.updatePassword(userId, PasswordUtil.hash(newPassword));
    }

    public void logout() {
        SessionManager.getInstance().logout();
    }
}
