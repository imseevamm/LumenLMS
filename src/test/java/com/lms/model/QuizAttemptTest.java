package com.lms.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class QuizAttemptTest {
    @Test
    void calculatesPercentage() {
        QuizAttempt attempt = new QuizAttempt();
        attempt.setScore(18);
        attempt.setTotalMarks(20);
        assertEquals(90.0, attempt.percentage(), 0.001);
    }

    @Test
    void avoidsDivisionByZero() {
        QuizAttempt attempt = new QuizAttempt();
        attempt.setScore(10);
        attempt.setTotalMarks(0);
        assertEquals(0.0, attempt.percentage(), 0.001);
    }
}
