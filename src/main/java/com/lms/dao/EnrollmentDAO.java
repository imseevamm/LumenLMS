package com.lms.dao;

import com.lms.model.Enrollment;
import com.lms.util.DBConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class EnrollmentDAO {

    private static final String BASE_SELECT =
            "SELECT en.*, c.title AS course_title, u.full_name AS instructor_name, su.full_name AS student_name, c.instructor_id " +
            "FROM enrollments en " +
            "JOIN courses c ON en.course_id = c.course_id " +
            "JOIN users u ON c.instructor_id = u.user_id JOIN users su ON en.student_id = su.user_id ";

    public List<Enrollment> findByStudent(long studentId) {
        List<Enrollment> list = new ArrayList<>();
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(BASE_SELECT + " WHERE en.student_id=? ORDER BY en.enrolled_at DESC")) {
            ps.setLong(1, studentId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(map(rs));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to load enrollments", e);
        }
        return list;
    }

    public boolean isEnrolled(long studentId, long courseId) {
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement("SELECT 1 FROM enrollments WHERE student_id=? AND course_id=?")) {
            ps.setLong(1, studentId);
            ps.setLong(2, courseId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to check enrollment", e);
        }
    }

    public Optional<Enrollment> findById(long enrollmentId) {
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(BASE_SELECT + " WHERE en.enrollment_id=?")) {
            ps.setLong(1, enrollmentId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return Optional.of(map(rs));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to load enrollment", e);
        }
        return Optional.empty();
    }

    public Optional<Enrollment> find(long studentId, long courseId) {
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(BASE_SELECT + " WHERE en.student_id=? AND en.course_id=?")) {
            ps.setLong(1, studentId);
            ps.setLong(2, courseId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return Optional.of(map(rs));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to load enrollment", e);
        }
        return Optional.empty();
    }

    public Enrollment enroll(long studentId, long courseId) {
        String sql = "INSERT INTO enrollments (student_id, course_id) VALUES (?,?)";
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setLong(1, studentId);
            ps.setLong(2, courseId);
            ps.executeUpdate();
            long id;
            try (ResultSet keys = ps.getGeneratedKeys()) {
                keys.next();
                id = keys.getLong(1);
            }
            return find(studentId, courseId).orElseGet(() -> {
                Enrollment e = new Enrollment();
                e.setId(id);
                e.setStudentId(studentId);
                e.setCourseId(courseId);
                return e;
            });
        } catch (SQLException e) {
            throw new RuntimeException("Failed to enroll student", e);
        }
    }

    public void updateStatus(long enrollmentId, Enrollment.Status status) {
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement("UPDATE enrollments SET status=? WHERE enrollment_id=?")) {
            ps.setString(1, status.name());
            ps.setLong(2, enrollmentId);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to update enrollment status", e);
        }
    }

    public List<Enrollment> findByCourse(long courseId) {
        List<Enrollment> list = new ArrayList<>();
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(BASE_SELECT + " WHERE en.course_id=? ORDER BY en.enrolled_at DESC")) {
            ps.setLong(1, courseId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(map(rs));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to load course enrollments", e);
        }
        return list;
    }

    public int countAll() {
        try (Connection con = DBConnection.getConnection();
             Statement st = con.createStatement();
             ResultSet rs = st.executeQuery("SELECT COUNT(*) FROM enrollments")) {
            if (rs.next()) return rs.getInt(1);
        } catch (SQLException e) {
            throw new RuntimeException("Failed to count enrollments", e);
        }
        return 0;
    }

    public double completionRate() {
        String sql = "SELECT " +
                "(SELECT COUNT(*) FROM enrollments WHERE status='COMPLETED') AS completed, " +
                "(SELECT COUNT(*) FROM enrollments) AS total";
        try (Connection con = DBConnection.getConnection();
             Statement st = con.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            if (rs.next()) {
                int total = rs.getInt("total");
                if (total == 0) return 0;
                return (rs.getInt("completed") * 100.0) / total;
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to compute completion rate", e);
        }
        return 0;
    }

    private Enrollment map(ResultSet rs) throws SQLException {
        Enrollment e = new Enrollment();
        e.setId(rs.getLong("enrollment_id"));
        e.setStudentId(rs.getLong("student_id"));
        e.setCourseId(rs.getLong("course_id"));
        e.setCourseTitle(rs.getString("course_title"));
        e.setInstructorName(rs.getString("instructor_name"));
        e.setStudentName(rs.getString("student_name"));
        Timestamp ts = rs.getTimestamp("enrolled_at");
        if (ts != null) e.setEnrolledAt(ts.toLocalDateTime());
        e.setStatus(Enrollment.Status.valueOf(rs.getString("status")));
        return e;
    }
}
