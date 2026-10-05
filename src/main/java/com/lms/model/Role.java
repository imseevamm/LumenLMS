package com.lms.model;

/** The three roles supported by the platform. Demonstrates use of enums for type-safe state. */
public enum Role {
    ADMIN, INSTRUCTOR, STUDENT;

    public String display() {
        return switch (this) {
            case ADMIN -> "Administrator";
            case INSTRUCTOR -> "Instructor";
            case STUDENT -> "Student";
        };
    }
}
