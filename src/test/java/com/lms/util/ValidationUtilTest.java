package com.lms.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ValidationUtilTest {
    @Test
    void validatesEmailAndBlankValues() {
        assertTrue(ValidationUtil.isValidEmail("student@example.com"));
        assertTrue(ValidationUtil.isValidEmail("  student@example.com  "));
        assertFalse(ValidationUtil.isValidEmail("student@example"));
        assertFalse(ValidationUtil.isNotBlank("   "));
        assertTrue(ValidationUtil.isNotBlank("Course title"));
    }

    @Test
    void validatesPasswordRulesAndMatching() {
        assertTrue(ValidationUtil.isStrongEnough("Password1"));
        assertFalse(ValidationUtil.isStrongEnough("password"));
        assertFalse(ValidationUtil.isStrongEnough("12345678"));
        assertTrue(ValidationUtil.passwordsMatch("Password1", "Password1"));
        assertFalse(ValidationUtil.passwordsMatch("Password1", "Password2"));
    }
}
