package com.lms.dao;

import com.lms.model.Submission;
import com.lms.util.DBConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class SubmissionDAO {

    private static final String BASE_SELECT =
            "SELECT s.*, a.title AS assignment_title, u.full_name AS student_name " +
            "FROM submissions s " +
            "JOIN assignments a ON s.assignment_id = a.assignment_id " +
            "JOIN users u ON s.student_id = u.user_id ";

    public Optional<Submission> findByAssignmentAndStudent(long assignmentId, long studentId) {
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(BASE_SELECT + " WHERE s.assignment_id=? AND s.student_id=?")) {
            ps.setLong(1, assignmentId);
            ps.setLong(2, studentId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return Optional.of(map(rs));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to load submission", e);
        }
        return Optional.empty();
    }


    public Optional<Submission> findById(long submissionId) {
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(BASE_SELECT + " WHERE s.submission_id=?")) {
            ps.setLong(1, submissionId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return Optional.of(map(rs));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to load submission", e);
        }
        return Optional.empty();
    }

    public List<Submission> findByAssignment(long assignmentId) {
        List<Submission> list = new ArrayList<>();
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(BASE_SELECT + " WHERE s.assignment_id=? ORDER BY s.submitted_at DESC")) {
            ps.setLong(1, assignmentId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(map(rs));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to load submissions", e);
        }
        return list;
    }

    public List<Submission> findByStudent(long studentId) {
        List<Submission> list = new ArrayList<>();
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(BASE_SELECT + " WHERE s.student_id=? ORDER BY s.submitted_at DESC")) {
            ps.setLong(1, studentId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(map(rs));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to load student submissions", e);
        }
        return list;
    }

    public Submission insert(Submission s) {
        try (Connection con = DBConnection.getConnection()) {
            if (hasAttachmentColumns(con)) {
                String sql = "INSERT INTO submissions (assignment_id, student_id, submission_text, attachment_path, attachment_name, attachment_size, status) VALUES (?,?,?,?,?,?,?)";
                try (PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
                    ps.setLong(1, s.getAssignmentId());
                    ps.setLong(2, s.getStudentId());
                    ps.setString(3, s.getSubmissionText());
                    ps.setString(4, s.getAttachmentPath());
                    ps.setString(5, s.getAttachmentName());
                    ps.setLong(6, s.getAttachmentSize());
                    ps.setString(7, s.getStatus().name());
                    ps.executeUpdate();
                    try (ResultSet keys = ps.getGeneratedKeys()) {
                        if (keys.next()) s.setId(keys.getLong(1));
                    }
                }
            } else {
                if (s.getAttachmentPath() != null) {
                    throw new RuntimeException("PDF attachments require the latest database migration. Run database/migrate_pdf_submission_attachments.sql once.");
                }
                String sql = "INSERT INTO submissions (assignment_id, student_id, submission_text, status) VALUES (?,?,?,?)";
                try (PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
                    ps.setLong(1, s.getAssignmentId());
                    ps.setLong(2, s.getStudentId());
                    ps.setString(3, s.getSubmissionText());
                    ps.setString(4, s.getStatus().name());
                    ps.executeUpdate();
                    try (ResultSet keys = ps.getGeneratedKeys()) {
                        if (keys.next()) s.setId(keys.getLong(1));
                    }
                }
            }
            return s;
        } catch (SQLException e) {
            throw new RuntimeException("Failed to submit assignment", e);
        }
    }

    public void updateAttachmentPath(long submissionId, String attachmentPath) {
        String sql = "UPDATE submissions SET attachment_path=? WHERE submission_id=?";
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, attachmentPath);
            ps.setLong(2, submissionId);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to update submission attachment", e);
        }
    }

    public void grade(long submissionId, int grade, String feedback) {
        String sql = "UPDATE submissions SET grade=?, feedback=?, status='GRADED' WHERE submission_id=?";
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, grade);
            ps.setString(2, feedback);
            ps.setLong(3, submissionId);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to grade submission", e);
        }
    }

    private Submission map(ResultSet rs) throws SQLException {
        Submission s = new Submission();
        s.setId(rs.getLong("submission_id"));
        s.setAssignmentId(rs.getLong("assignment_id"));
        s.setAssignmentTitle(rs.getString("assignment_title"));
        s.setStudentId(rs.getLong("student_id"));
        s.setStudentName(rs.getString("student_name"));
        s.setSubmissionText(rs.getString("submission_text"));
        s.setAttachmentPath(getNullableString(rs, "attachment_path"));
        s.setAttachmentName(getNullableString(rs, "attachment_name"));
        s.setAttachmentSize(getNullableLong(rs, "attachment_size"));
        Timestamp ts = rs.getTimestamp("submitted_at");
        if (ts != null) s.setSubmittedAt(ts.toLocalDateTime());
        int grade = rs.getInt("grade");
        s.setGrade(rs.wasNull() ? null : grade);
        s.setFeedback(rs.getString("feedback"));
        s.setStatus(Submission.Status.valueOf(rs.getString("status")));
        return s;
    }

    private boolean hasAttachmentColumns(Connection con) throws SQLException {
        DatabaseMetaData meta = con.getMetaData();
        String catalog = con.getCatalog();
        try (ResultSet rs = meta.getColumns(catalog, null, "submissions", "attachment_path")) {
            return rs.next();
        }
    }

    private String getNullableString(ResultSet rs, String column) {
        try {
            return rs.getString(column);
        } catch (SQLException ignored) {
            return null;
        }
    }

    private long getNullableLong(ResultSet rs, String column) {
        try {
            long value = rs.getLong(column);
            return rs.wasNull() ? 0L : value;
        } catch (SQLException ignored) {
            return 0L;
        }
    }
}
