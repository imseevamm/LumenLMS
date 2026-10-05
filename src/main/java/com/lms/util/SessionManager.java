package com.lms.util;

import com.lms.model.Role;
import com.lms.model.User;
import com.lms.security.AuthorizationException;

import java.util.Arrays;

/**
 * Holds the currently authenticated user for the lifetime of the application instance.
 * Simple singleton - appropriate for a single-user desktop client session.
 */
public final class SessionManager {

    private static SessionManager instance;

    // Written by the login worker thread, read by the FX thread and background tasks.
    private volatile User currentUser;

    private SessionManager() {}

    public static synchronized SessionManager getInstance() {
        if (instance == null) instance = new SessionManager();
        return instance;
    }

    public User getCurrentUser() { return currentUser; }
    public void setCurrentUser(User user) { this.currentUser = user; }
    public boolean isLoggedIn() { return currentUser != null; }

    public void logout() { currentUser = null; }

    /** Service-layer guard: returns the caller if signed in with one of the allowed roles, otherwise throws. */
    public User requireRole(Role... allowed) {
        User caller = currentUser;
        if (caller == null) throw new AuthorizationException("You must be signed in to do that.");
        if (Arrays.asList(allowed).contains(caller.getRole())) return caller;
        throw new AuthorizationException("You do not have permission to do that.");
    }
}
