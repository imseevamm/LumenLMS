package com.lms.security;

/** Thrown when the signed-in user is not allowed to perform a service-level operation. */
public class AuthorizationException extends RuntimeException {

    public AuthorizationException(String message) {
        super(message);
    }
}
