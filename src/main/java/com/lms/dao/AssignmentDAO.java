package com.lms.dao;

import com.lms.model.Assignment;
import com.lms.util.DBConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class AssignmentDAO {

    private static final String BASE_SELECT =
            "SELECT a.*, c.title AS course_title FROM assignments a JOIN courses c ON a.course_id = c.course_id ";

    public List<Assignment> findByCourse(long courseId) {
        List<Assignment> list = new ArrayList<>();
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(BASE_SELECT + " WHERE a.course_id=? ORDER BY a.deadline")) {
            ps.setLong(1, courseId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(map(rs));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to load assignments", e);
        }
        return list;
    }

    public List<Assignment> findByStudentEnrollments(long studentId) {
        List<Assignment> list = new ArrayList<>();
        String sql = BASE_SELECT + " JOIN enrollments e ON e.course_id = a.course_id " +
                "WHERE e.student_id=? ORDER BY a.deadline";
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setLong(1, studentId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(map(rs));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to load student assignments", e);
        }
        return list;
    }

    public Optional<Assignment> findById(long id) {
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(BASE_SELECT + " WHERE a.assignment_id=?")) {
            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return Optional.of(map(rs));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to load assignment", e);
        }
        return Optional.empty();
    }

    public Assignment insert(Assignment a) {
        String sql = "INSERT INTO assignments (course_id, title, description, instructions, deadline, max_marks) VALUES (?,?,?,?,?,?)";
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setLong(1, a.getCourseId());
            ps.setString(2, a.getTitle());
            ps.setString(3, a.getDescription());
            ps.setString(4, a.getInstructions());
            ps.setTimestamp(5, Timestamp.valueOf(a.getDeadline()));
            ps.setInt(6, a.getMaxMarks());
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) a.setId(keys.getLong(1));
            }
            return a;
        } catch (SQLException e) {
            throw new RuntimeException("Failed to insert assignment", e);
        }
    }

    public void update(Assignment a) {
        String sql = "UPDATE assignments SET title=?, description=?, instructions=?, deadline=?, max_marks=? WHERE assignment_id=?";
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, a.getTitle());
            ps.setString(2, a.getDescription());
            ps.setString(3, a.getInstructions());
            ps.setTimestamp(4, Timestamp.valueOf(a.getDeadline()));
            ps.setInt(5, a.getMaxMarks());
            ps.setLong(6, a.getId());
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to update assignment", e);
        }
    }

    public void delete(long id) {
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement("DELETE FROM assignments WHERE assignment_id=?")) {
            ps.setLong(1, id);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to delete assignment", e);
        }
    }

    public int countAll() {
        try (Connection con = DBConnection.getConnection();
             Statement st = con.createStatement();
             ResultSet rs = st.executeQuery("SELECT COUNT(*) FROM assignments")) {
            if (rs.next()) return rs.getInt(1);
        } catch (SQLException e) {
            throw new RuntimeException("Failed to count assignments", e);
        }
        return 0;
    }

    private Assignment map(ResultSet rs) throws SQLException {
        Assignment a = new Assignment();
        a.setId(rs.getLong("assignment_id"));
        a.setCourseId(rs.getLong("course_id"));
        a.setCourseTitle(rs.getString("course_title"));
        a.setTitle(rs.getString("title"));
        a.setDescription(rs.getString("description"));
        a.setInstructions(rs.getString("instructions"));
        Timestamp dl = rs.getTimestamp("deadline");
        if (dl != null) a.setDeadline(dl.toLocalDateTime());
        a.setMaxMarks(rs.getInt("max_marks"));
        Timestamp created = rs.getTimestamp("created_at");
        if (created != null) a.setCreatedAt(created.toLocalDateTime());
        return a;
    }
}
