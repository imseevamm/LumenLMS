package com.lms.dao;

import com.lms.util.DBConnection;

import java.sql.*;
import java.util.HashSet;
import java.util.Set;

public class ProgressDAO {

    /** Returns the set of completed lesson ids for a given enrollment. */
    public Set<Long> completedLessonIds(long enrollmentId) {
        Set<Long> ids = new HashSet<>();
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(
                     "SELECT lesson_id FROM progress WHERE enrollment_id=? AND is_completed=TRUE")) {
            ps.setLong(1, enrollmentId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) ids.add(rs.getLong(1));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to load progress", e);
        }
        return ids;
    }

    public void markLessonComplete(long enrollmentId, long lessonId) {
        String sql = "INSERT INTO progress (enrollment_id, lesson_id, is_completed, completed_at) VALUES (?,?,TRUE,NOW()) " +
                "ON DUPLICATE KEY UPDATE is_completed=TRUE, completed_at=NOW()";
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setLong(1, enrollmentId);
            ps.setLong(2, lessonId);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to mark lesson complete", e);
        }
    }

    public double progressPercent(long enrollmentId, int totalLessons) {
        if (totalLessons == 0) return 0;
        return (completedLessonIds(enrollmentId).size() * 100.0) / totalLessons;
    }
}
