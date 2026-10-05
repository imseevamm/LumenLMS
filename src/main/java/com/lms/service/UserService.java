package com.lms.service;

import com.lms.dao.UserDAO;
import com.lms.model.Role;
import com.lms.model.User;
import com.lms.security.AuthorizationException;
import com.lms.security.PasswordUtil;
import com.lms.util.SessionManager;
import com.lms.util.ValidationUtil;

import java.util.List;

/** Business logic for admin-driven user management (including admin provisioning). */
public class UserService {

    private final UserDAO userDAO = new UserDAO();

    public static class UserServiceException extends RuntimeException {
        public UserServiceException(String message) { super(message); }
    }

    public List<User> getAllUsers() {
        requireAdmin();
        return userDAO.findAll();
    }

    public List<User> search(String keyword, Role roleFilter) {
        requireAdmin();
        if (!ValidationUtil.isNotBlank(keyword)) {
            return roleFilter == null ? userDAO.findAll() : userDAO.findByRole(roleFilter);
        }
        return userDAO.search(keyword.trim(), roleFilter);
    }

    /** Only administrators call this - it is the sole path capable of creating ADMIN accounts. */
    public User createUser(String fullName, String email, String password, Role role) {
        requireAdmin();
        if (!ValidationUtil.isNotBlank(fullName)) throw new UserServiceException("Full name is required.");
        if (!ValidationUtil.isValidEmail(email)) throw new UserServiceException("Please enter a valid email address.");
        if (!ValidationUtil.isStrongEnough(password)) throw new UserServiceException("Password does not meet strength requirements.");
        if (userDAO.findByEmail(email.trim().toLowerCase()).isPresent()) {
            throw new UserServiceException("A user with this email already exists.");
        }
        User u = new User();
        u.setFullName(fullName.trim());
        u.setEmail(email.trim().toLowerCase());
        u.setPasswordHash(PasswordUtil.hash(password));
        u.setRole(role);
        u.setActive(true);
        return userDAO.insert(u);
    }

    /** Admins may edit anyone; everyone else may only edit their own profile and never their role or status. */
    public void updateUser(User user) {
        User caller = SessionManager.getInstance().requireRole(Role.values());
        if (!ValidationUtil.isNotBlank(user.getFullName())) throw new UserServiceException("Full name is required.");
        if (!ValidationUtil.isValidEmail(user.getEmail())) throw new UserServiceException("Please enter a valid email address.");
        userDAO.findByEmail(user.getEmail().trim().toLowerCase()).ifPresent(other -> {
            if (other.getId() != user.getId()) {
                throw new UserServiceException("Another account already uses this email address.");
            }
        });
        if (caller.getRole() == Role.ADMIN) {
            if (user.getId() == caller.getId() && (user.getRole() != Role.ADMIN || !user.isActive())) {
                throw new UserServiceException("You cannot remove your own administrator access.");
            }
        } else {
            keepStoredRoleAndStatus(caller, user);
        }
        userDAO.update(user);
    }

    /** Allows an administrator to reset another user's password without knowing the old password. */
    public void resetPassword(long userId, String newPassword) {
        requireAdmin();
        if (!ValidationUtil.isStrongEnough(newPassword)) {
            throw new UserServiceException("Password must be at least 8 characters and include a letter and a number.");
        }
        if (userDAO.findById(userId).isEmpty()) {
            throw new UserServiceException("User not found.");
        }
        userDAO.updatePassword(userId, PasswordUtil.hash(newPassword));
    }

    public void toggleActive(long userId, boolean active) {
        User caller = requireAdmin();
        if (!active && caller.getId() == userId) throw new UserServiceException("You cannot deactivate your own account.");
        userDAO.setActive(userId, active);
    }

    public void deleteUser(long userId) {
        User caller = requireAdmin();
        if (caller.getId() == userId) throw new UserServiceException("You cannot delete your own account.");
        userDAO.delete(userId);
    }

    public int countByRole(Role role) {
        requireAdmin();
        return userDAO.countByRole(role);
    }

    private User requireAdmin() {
        return SessionManager.getInstance().requireRole(Role.ADMIN);
    }

    private void keepStoredRoleAndStatus(User caller, User edited) {
        if (caller.getId() != edited.getId()) {
            throw new AuthorizationException("You can only edit your own profile.");
        }
        User stored = userDAO.findById(edited.getId())
                .orElseThrow(() -> new UserServiceException("User not found."));
        edited.setRole(stored.getRole());
        edited.setActive(stored.isActive());
    }
}
