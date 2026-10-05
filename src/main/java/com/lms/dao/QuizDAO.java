package com.lms.dao;

import com.lms.model.*;
import com.lms.util.DBConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class QuizDAO {

    public List<Quiz> findByCourse(long courseId) {
        List<Quiz> quizzes = new ArrayList<>();
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement("SELECT * FROM quizzes WHERE course_id=? ORDER BY created_at DESC")) {
            ps.setLong(1, courseId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) quizzes.add(mapQuiz(rs));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to load quizzes", e);
        }
        return quizzes;
    }

    public Optional<Quiz> findByIdWithQuestions(long quizId) {
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement("SELECT * FROM quizzes WHERE quiz_id=?")) {
            ps.setLong(1, quizId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    Quiz q = mapQuiz(rs);
                    q.setQuestions(findQuestions(quizId));
                    return Optional.of(q);
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to load quiz", e);
        }
        return Optional.empty();
    }

    public Optional<Long> findCourseInstructorId(long courseId) {
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement("SELECT instructor_id FROM courses WHERE course_id=?")) {
            ps.setLong(1, courseId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return Optional.of(rs.getLong(1));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to verify course ownership", e);
        }
        return Optional.empty();
    }

    public Optional<Quiz> findByQuestionId(long questionId) {
        String sql = "SELECT q.* FROM quizzes q JOIN quiz_questions qq ON q.quiz_id=qq.quiz_id WHERE qq.question_id=?";
        try (Connection con = DBConnection.getConnection(); PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setLong(1, questionId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return Optional.of(mapQuiz(rs));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to find question quiz", e);
        }
        return Optional.empty();
    }

    public List<QuizQuestion> findQuestions(long quizId) {
        List<QuizQuestion> questions = new ArrayList<>();
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement("SELECT * FROM quiz_questions WHERE quiz_id=? ORDER BY position")) {
            ps.setLong(1, quizId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    QuizQuestion q = new QuizQuestion();
                    q.setId(rs.getLong("question_id"));
                    q.setQuizId(rs.getLong("quiz_id"));
                    q.setQuestionText(rs.getString("question_text"));
                    q.setOptionA(rs.getString("option_a"));
                    q.setOptionB(rs.getString("option_b"));
                    q.setOptionC(rs.getString("option_c"));
                    q.setOptionD(rs.getString("option_d"));
                    q.setCorrectOption(rs.getString("correct_option").charAt(0));
                    q.setMarks(rs.getInt("marks"));
                    q.setPosition(rs.getInt("position"));
                    questions.add(q);
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to load quiz questions", e);
        }
        return questions;
    }

    public Quiz insertQuiz(Quiz q) {
        String sql = "INSERT INTO quizzes (course_id, title, description, duration_minutes, is_published) VALUES (?,?,?,?,?)";
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setLong(1, q.getCourseId());
            ps.setString(2, q.getTitle());
            ps.setString(3, q.getDescription());
            ps.setInt(4, q.getDurationMinutes());
            ps.setBoolean(5, q.isPublished());
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) q.setId(keys.getLong(1));
            }
            return q;
        } catch (SQLException e) {
            throw new RuntimeException("Failed to insert quiz", e);
        }
    }

    public void updateQuiz(Quiz q) {
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(
                     "UPDATE quizzes SET title=?, description=?, duration_minutes=?, is_published=? WHERE quiz_id=?")) {
            ps.setString(1, q.getTitle());
            ps.setString(2, q.getDescription());
            ps.setInt(3, q.getDurationMinutes());
            ps.setBoolean(4, q.isPublished());
            ps.setLong(5, q.getId());
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to update quiz", e);
        }
    }

    public void deleteQuiz(long quizId) {
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement("DELETE FROM quizzes WHERE quiz_id=?")) {
            ps.setLong(1, quizId);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to delete quiz", e);
        }
    }

    public QuizQuestion insertQuestion(QuizQuestion q) {
        String sql = "INSERT INTO quiz_questions (quiz_id, question_text, option_a, option_b, option_c, option_d, correct_option, marks, position) " +
                "VALUES (?,?,?,?,?,?,?,?,?)";
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setLong(1, q.getQuizId());
            ps.setString(2, q.getQuestionText());
            ps.setString(3, q.getOptionA());
            ps.setString(4, q.getOptionB());
            ps.setString(5, q.getOptionC());
            ps.setString(6, q.getOptionD());
            ps.setString(7, String.valueOf(q.getCorrectOption()));
            ps.setInt(8, q.getMarks());
            ps.setInt(9, q.getPosition());
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) q.setId(keys.getLong(1));
            }
            return q;
        } catch (SQLException e) {
            throw new RuntimeException("Failed to insert question", e);
        }
    }

    public void deleteQuestion(long questionId) {
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement("DELETE FROM quiz_questions WHERE question_id=?")) {
            ps.setLong(1, questionId);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to delete question", e);
        }
    }

    public QuizAttempt insertAttempt(QuizAttempt attempt) {
        String sql = "INSERT INTO quiz_attempts (quiz_id, student_id, score, total_marks, submitted_at) VALUES (?,?,?,?,NOW())";
        try (Connection con = DBConnection.getConnection()) {
            boolean oldAutoCommit = con.getAutoCommit();
            con.setAutoCommit(false);
            try (PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
                ps.setLong(1, attempt.getQuizId());
                ps.setLong(2, attempt.getStudentId());
                ps.setDouble(3, attempt.getScore());
                ps.setDouble(4, attempt.getTotalMarks());
                ps.executeUpdate();
                long attemptId;
                try (ResultSet keys = ps.getGeneratedKeys()) {
                    if (!keys.next()) throw new SQLException("Database did not return the quiz attempt id.");
                    attemptId = keys.getLong(1);
                    attempt.setId(attemptId);
                }
                try (PreparedStatement aps = con.prepareStatement(
                        "INSERT INTO quiz_answers (attempt_id, question_id, selected_option, is_correct) VALUES (?,?,?,?)")) {
                    for (QuizAnswer ans : attempt.getAnswers()) {
                        aps.setLong(1, attemptId);
                        aps.setLong(2, ans.getQuestionId());
                        if (ans.getSelectedOption() != null) aps.setString(3, String.valueOf(ans.getSelectedOption()));
                        else aps.setNull(3, Types.CHAR);
                        aps.setBoolean(4, ans.isCorrect());
                        aps.addBatch();
                    }
                    aps.executeBatch();
                }
                con.commit();
                con.setAutoCommit(oldAutoCommit);
                return attempt;
            } catch (SQLException ex) {
                try { con.rollback(); } catch (SQLException ignored) { }
                throw ex;
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to save quiz attempt", e);
        }
    }

    public List<QuizAttempt> findAttemptsByStudent(long studentId) {
        List<QuizAttempt> attempts = new ArrayList<>();
        String sql = "SELECT qa.*, q.title AS quiz_title FROM quiz_attempts qa " +
                "JOIN quizzes q ON qa.quiz_id = q.quiz_id WHERE qa.student_id=? ORDER BY qa.submitted_at DESC";
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setLong(1, studentId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) attempts.add(mapAttempt(rs));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to load quiz attempts", e);
        }
        return attempts;
    }

    /** Returns completed quiz attempts for one student within one course. */
    public List<QuizAttempt> findAttemptsByStudentAndCourse(long studentId, long courseId) {
        List<QuizAttempt> attempts = new ArrayList<>();
        String sql = "SELECT qa.*, q.title AS quiz_title FROM quiz_attempts qa " +
                "JOIN quizzes q ON qa.quiz_id = q.quiz_id " +
                "WHERE qa.student_id=? AND q.course_id=? ORDER BY qa.submitted_at DESC";
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setLong(1, studentId);
            ps.setLong(2, courseId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) attempts.add(mapAttempt(rs));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to load course quiz attempts", e);
        }
        return attempts;
    }

    public boolean hasAttempted(long studentId, long quizId) {
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement("SELECT 1 FROM quiz_attempts WHERE student_id=? AND quiz_id=?")) {
            ps.setLong(1, studentId);
            ps.setLong(2, quizId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to check quiz attempt", e);
        }
    }

    public double averageScorePercent(long courseId) {
        String sql = "SELECT AVG(qa.score / NULLIF(qa.total_marks,0) * 100) AS avg_pct FROM quiz_attempts qa " +
                "JOIN quizzes q ON qa.quiz_id = q.quiz_id WHERE q.course_id=?";
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setLong(1, courseId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getDouble("avg_pct");
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to compute average quiz score", e);
        }
        return 0;
    }

    private Quiz mapQuiz(ResultSet rs) throws SQLException {
        Quiz q = new Quiz();
        q.setId(rs.getLong("quiz_id"));
        q.setCourseId(rs.getLong("course_id"));
        q.setTitle(rs.getString("title"));
        q.setDescription(rs.getString("description"));
        q.setDurationMinutes(rs.getInt("duration_minutes"));
        q.setPublished(rs.getBoolean("is_published"));
        return q;
    }

    private QuizAttempt mapAttempt(ResultSet rs) throws SQLException {
        QuizAttempt a = new QuizAttempt();
        a.setId(rs.getLong("attempt_id"));
        a.setQuizId(rs.getLong("quiz_id"));
        a.setQuizTitle(rs.getString("quiz_title"));
        a.setStudentId(rs.getLong("student_id"));
        a.setScore(rs.getDouble("score"));
        a.setTotalMarks(rs.getDouble("total_marks"));
        Timestamp sub = rs.getTimestamp("submitted_at");
        if (sub != null) a.setSubmittedAt(sub.toLocalDateTime());
        return a;
    }
}
