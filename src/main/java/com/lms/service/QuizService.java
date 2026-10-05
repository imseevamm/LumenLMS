package com.lms.service;

import com.lms.dao.EnrollmentDAO;
import com.lms.dao.QuizDAO;
import com.lms.model.*;
import com.lms.security.AuthorizationException;
import com.lms.util.SessionManager;
import com.lms.util.ValidationUtil;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public class QuizService {

    /** Extra seconds allowed after the nominal time limit to absorb UI/DB latency before an attempt counts as late. */
    private static final long SUBMIT_GRACE_SECONDS = 90;

    /**
     * When each in-progress attempt started, keyed by "studentId:quizId". Shared by the FX thread and
     * background workers, hence a ConcurrentHashMap. This is what makes the time limit enforceable:
     * the clock starts on the server side of the app, not in the UI widget.
     */
    private static final Map<String, Instant> ATTEMPT_STARTS = new ConcurrentHashMap<>();

    private final QuizDAO quizDAO = new QuizDAO();
    private final EnrollmentDAO enrollmentDAO = new EnrollmentDAO();

    public static class QuizException extends RuntimeException {
        public QuizException(String message) { super(message); }
    }

    public List<Quiz> getQuizzesForCourse(long courseId) {
        User caller = SessionManager.getInstance().requireRole(Role.ADMIN, Role.INSTRUCTOR, Role.STUDENT);
        if (caller.getRole() == Role.INSTRUCTOR) requireInstructorCourse(caller, courseId);
        if (caller.getRole() == Role.STUDENT && !enrollmentDAO.isEnrolled(caller.getId(), courseId)) {
            throw new AuthorizationException("You must be enrolled in this course to view its quizzes.");
        }
        List<Quiz> quizzes = quizDAO.findByCourse(courseId);
        return caller.getRole() == Role.STUDENT ? quizzes.stream().filter(Quiz::isPublished).toList() : quizzes;
    }

    public Optional<Quiz> getQuizWithQuestions(long quizId) {
        User caller = SessionManager.getInstance().requireRole(Role.ADMIN, Role.INSTRUCTOR, Role.STUDENT);
        Optional<Quiz> quiz = quizDAO.findByIdWithQuestions(quizId);
        if (quiz.isEmpty()) return Optional.empty();
        if (caller.getRole() == Role.INSTRUCTOR) requireInstructorCourse(caller, quiz.get().getCourseId());
        if (caller.getRole() == Role.STUDENT) {
            if (!quiz.get().isPublished()) throw new AuthorizationException("This quiz is not available yet.");
            if (!enrollmentDAO.isEnrolled(caller.getId(), quiz.get().getCourseId())) {
                throw new AuthorizationException("You must be enrolled in this course to access this quiz.");
            }
        }
        return quiz;
    }

    public Quiz createQuiz(long courseId, String title, String description, int duration) {
        User caller = SessionManager.getInstance().requireRole(Role.ADMIN, Role.INSTRUCTOR);
        if (caller.getRole() == Role.INSTRUCTOR) requireInstructorCourse(caller, courseId);
        if (!ValidationUtil.isNotBlank(title)) throw new QuizException("Quiz title is required.");
        if (duration < 1 || duration > 480) throw new QuizException("Quiz duration must be between 1 and 480 minutes.");
        Quiz q = new Quiz();
        q.setCourseId(courseId);
        q.setTitle(title.trim());
        q.setDescription(description == null ? null : description.trim());
        q.setDurationMinutes(duration);
        q.setPublished(false);
        return quizDAO.insertQuiz(q);
    }

    public void updateQuiz(Quiz quiz) {
        User caller = SessionManager.getInstance().requireRole(Role.ADMIN, Role.INSTRUCTOR);
        if (quiz == null) throw new QuizException("Quiz is required.");
        Quiz existing = quizDAO.findByIdWithQuestions(quiz.getId()).orElseThrow(() -> new QuizException("Quiz not found."));
        if (caller.getRole() == Role.INSTRUCTOR) requireInstructorCourse(caller, existing.getCourseId());
        if (!ValidationUtil.isNotBlank(quiz.getTitle())) throw new QuizException("Quiz title is required.");
        if (quiz.getDurationMinutes() < 1 || quiz.getDurationMinutes() > 480) throw new QuizException("Quiz duration must be between 1 and 480 minutes.");
        quiz.setCourseId(existing.getCourseId());
        quiz.setTitle(quiz.getTitle().trim());
        quiz.setDescription(quiz.getDescription() == null ? null : quiz.getDescription().trim());
        quizDAO.updateQuiz(quiz);
    }

    public void togglePublish(Quiz quiz) {
        User caller = SessionManager.getInstance().requireRole(Role.ADMIN, Role.INSTRUCTOR);
        if (caller.getRole() == Role.INSTRUCTOR) requireInstructorCourse(caller, quiz.getCourseId());
        if (!quiz.isPublished() && quizDAO.findQuestions(quiz.getId()).isEmpty()) {
            throw new QuizException("Add at least one question before publishing the quiz.");
        }
        quiz.setPublished(!quiz.isPublished());
        quizDAO.updateQuiz(quiz);
    }

    public void deleteQuiz(long quizId) {
        User caller = SessionManager.getInstance().requireRole(Role.ADMIN, Role.INSTRUCTOR);
        Quiz quiz = quizDAO.findByIdWithQuestions(quizId).orElseThrow(() -> new QuizException("Quiz not found."));
        if (caller.getRole() == Role.INSTRUCTOR) requireInstructorCourse(caller, quiz.getCourseId());
        quizDAO.deleteQuiz(quizId);
    }

    public QuizQuestion addQuestion(long quizId, String text, String a, String b, String c, String d,
                                     char correct, int marks, int position) {
        User caller = SessionManager.getInstance().requireRole(Role.ADMIN, Role.INSTRUCTOR);
        Quiz quiz = quizDAO.findByIdWithQuestions(quizId).orElseThrow(() -> new QuizException("Quiz not found."));
        if (caller.getRole() == Role.INSTRUCTOR) requireInstructorCourse(caller, quiz.getCourseId());
        if (!ValidationUtil.isNotBlank(text)) throw new QuizException("Question text is required.");
        if (!ValidationUtil.isNotBlank(a) || !ValidationUtil.isNotBlank(b) ||
                !ValidationUtil.isNotBlank(c) || !ValidationUtil.isNotBlank(d)) {
            throw new QuizException("All four options are required.");
        }
        char normalized = Character.toUpperCase(correct);
        if (normalized < 'A' || normalized > 'D') throw new QuizException("Correct option must be A, B, C, or D.");
        if (marks < 1) throw new QuizException("Marks must be at least 1.");
        QuizQuestion q = new QuizQuestion();
        q.setQuizId(quizId);
        q.setQuestionText(text.trim());
        q.setOptionA(a.trim()); q.setOptionB(b.trim()); q.setOptionC(c.trim()); q.setOptionD(d.trim());
        q.setCorrectOption(normalized);
        q.setMarks(marks);
        q.setPosition(Math.max(position, 1));
        return quizDAO.insertQuestion(q);
    }

    public void deleteQuestion(long questionId) {
        User caller = SessionManager.getInstance().requireRole(Role.ADMIN, Role.INSTRUCTOR);
        Quiz quiz = quizDAO.findByQuestionId(questionId).orElseThrow(() -> new QuizException("Question not found."));
        if (caller.getRole() == Role.INSTRUCTOR) requireInstructorCourse(caller, quiz.getCourseId());
        quizDAO.deleteQuestion(questionId);
    }

    /**
     * Starts (or resumes) the clock for a student's attempt and returns the original start time.
     * Re-opening the quiz does NOT reset the timer.
     */
    public Instant beginAttempt(long studentId, long quizId) {
        User caller = SessionManager.getInstance().requireRole(Role.STUDENT);
        if (caller.getId() != studentId) throw new AuthorizationException("You can only start your own quiz attempt.");
        Quiz current = quizDAO.findByIdWithQuestions(quizId).orElseThrow(() -> new QuizException("Quiz not found."));
        if (!current.isPublished()) throw new QuizException("This quiz is not available yet.");
        if (!enrollmentDAO.isEnrolled(studentId, current.getCourseId())) {
            throw new AuthorizationException("You must be enrolled in this course to attempt this quiz.");
        }
        if (quizDAO.hasAttempted(studentId, quizId)) throw new QuizException("You have already attempted this quiz.");
        return ATTEMPT_STARTS.computeIfAbsent(studentId + ":" + quizId, k -> Instant.now());
    }

    /** Grades a quiz attempt: answers is a map of questionId -> selected option ('A'-'D') or null if unanswered. */
    public QuizAttempt submitAttempt(long studentId, Quiz quiz, Map<Long, Character> answers) {
        User caller = SessionManager.getInstance().requireRole(Role.STUDENT);
        if (caller.getId() != studentId) throw new AuthorizationException("You can only submit your own quiz attempt.");
        Quiz current = quizDAO.findByIdWithQuestions(quiz.getId()).orElseThrow(() -> new QuizException("Quiz not found."));
        if (!current.isPublished()) throw new QuizException("This quiz is not available yet.");
        if (!enrollmentDAO.isEnrolled(studentId, current.getCourseId())) {
            throw new AuthorizationException("You must be enrolled in this course to attempt this quiz.");
        }
        if (quizDAO.hasAttempted(studentId, current.getId())) {
            throw new QuizException("You have already attempted this quiz.");
        }
        // Time-limit enforcement: if the attempt started on record and the limit (plus a small grace period)
        // has passed, answers are no longer accepted and the attempt is recorded with only unanswered questions.
        String attemptKey = studentId + ":" + current.getId();
        Instant started = ATTEMPT_STARTS.get(attemptKey);
        boolean expired = started != null
                && Instant.now().isAfter(started.plusSeconds(current.getDurationMinutes() * 60L + SUBMIT_GRACE_SECONDS));
        Map<Long, Character> safeAnswers = (answers == null || expired) ? Map.of() : answers;
        QuizAttempt attempt = new QuizAttempt();
        attempt.setQuizId(current.getId());
        attempt.setStudentId(studentId);

        double totalMarks = 0;
        double score = 0;
        for (QuizQuestion q : current.getQuestions()) {
            totalMarks += q.getMarks();
            Character selected = safeAnswers.get(q.getId());
            if (selected != null) {
                selected = Character.toUpperCase(selected);
                if (selected < 'A' || selected > 'D') selected = null;
            }
            boolean correct = selected != null && selected == q.getCorrectOption();
            if (correct) score += q.getMarks();
            QuizAnswer ans = new QuizAnswer();
            ans.setQuestionId(q.getId());
            ans.setSelectedOption(selected);
            ans.setCorrect(correct);
            attempt.getAnswers().add(ans);
        }
        attempt.setScore(score);
        attempt.setTotalMarks(totalMarks);
        QuizAttempt saved;
        try {
            saved = quizDAO.insertAttempt(attempt);
        } catch (RuntimeException ex) {
            for (Throwable t = ex; t != null; t = t.getCause()) {
                if (t instanceof java.sql.SQLIntegrityConstraintViolationException) {
                    throw new QuizException("You have already attempted this quiz.");
                }
            }
            throw ex;
        }
        ATTEMPT_STARTS.remove(attemptKey);
        return saved;
    }

    public List<QuizAttempt> getAttemptsForStudent(long studentId) {
        User caller = SessionManager.getInstance().requireRole(Role.ADMIN, Role.STUDENT);
        if (caller.getRole() == Role.STUDENT && caller.getId() != studentId) {
            throw new AuthorizationException("You can only view your own quiz attempts.");
        }
        return quizDAO.findAttemptsByStudent(studentId);
    }

    /** Attempts of one student within one course. Admins any; instructors own courses only; students themselves only. */
    public List<QuizAttempt> getAttemptsForStudentInCourse(long studentId, long courseId) {
        User caller = SessionManager.getInstance().requireRole(Role.ADMIN, Role.INSTRUCTOR, Role.STUDENT);
        if (caller.getRole() == Role.INSTRUCTOR) requireInstructorCourse(caller, courseId);
        if (caller.getRole() == Role.STUDENT && caller.getId() != studentId) {
            throw new AuthorizationException("You can only view your own quiz attempts.");
        }
        return quizDAO.findAttemptsByStudentAndCourse(studentId, courseId);
    }

    public boolean hasAttempted(long studentId, long quizId) {
        User caller = SessionManager.getInstance().requireRole(Role.ADMIN, Role.STUDENT);
        if (caller.getRole() == Role.STUDENT && caller.getId() != studentId) {
            throw new AuthorizationException("You can only check your own quiz attempts.");
        }
        return quizDAO.hasAttempted(studentId, quizId);
    }

    public double averageScoreForCourse(long courseId) {
        User caller = SessionManager.getInstance().requireRole(Role.ADMIN, Role.INSTRUCTOR);
        if (caller.getRole() == Role.INSTRUCTOR) requireInstructorCourse(caller, courseId);
        return quizDAO.averageScorePercent(courseId);
    }

    private void requireInstructorCourse(User instructor, long courseId) {
        long ownerId = quizDAO.findCourseInstructorId(courseId).orElseThrow(() -> new QuizException("Course not found."));
        if (ownerId != instructor.getId()) {
            throw new AuthorizationException("Instructors can only manage quizzes in their own courses.");
        }
    }
}
